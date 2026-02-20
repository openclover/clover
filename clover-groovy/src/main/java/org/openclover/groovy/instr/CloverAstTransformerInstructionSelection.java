package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.instr.tests.naming.JUnitParameterizedTestExtractor;
import org.openclover.core.instr.tests.naming.SpockFeatureNameExtractor;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.recorder.PerTestRecorder;
import org.openclover.runtime.recorder.pertest.SnifferType;
import org_openclover_runtime.CoverageRecorder;
import org_openclover_runtime.TestNameSniffer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static groovyjarjarasm.asm.Opcodes.ACC_FINAL;
import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;
import static groovyjarjarasm.asm.Opcodes.ACC_SYNTHETIC;
import static org.openclover.core.util.Maps.newHashMap;

/**
 * We attach to the instruction selection phase because
 * most ast transformers occur in the canonicalization phase
 */
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class CloverAstTransformerInstructionSelection extends CloverAstTransformerBase {

    /** See clover-groovy/src/main/assembly/META-INF/services/org.codehaus.groovy.transform.ASTTransformation */
    @SuppressWarnings("unused")
    public CloverAstTransformerInstructionSelection() throws IOException, CloverException {
        super(newConfigFromResource());
    }

    @Override
    public void visit(SourceUnit sourceUnit) {
        try {
            final ModuleNode module = sourceUnit.getAST();
            if (config != null && config.isEnabled() && !hasRecorderClass(module)) {
                if (!isIncluded(sourceUnit)) {
                    Logger.getInstance().verbose("Skipping " + getSourceUnitFile(sourceUnit));
                } else {

                    maybeDumpAST(module, sourceUnit, "Original source", ".before.instruction.txt");

                    final String pkg = sanitisePackageName(module.getPackageName());
                    final File srcFile = getSourceUnitFile(sourceUnit);
                    final TestDetector.SourceContext testSourceContext = new GroovySourceContext(srcFile);
                    final int lastLineNumber = module.getClasses() == null ? 0 : getLastLineNumber(module.getClasses());
                    final Map<ClassNode, GroovyInstrumentationResult> flagsForInstrumentedClasses = newHashMap();

                    // first pass - add recorder.inc() stuff
                    Logger.getInstance().verbose("Processing \"" + getSourceUnitFile(sourceUnit) + "\", package - \"" + pkg + "\"");
                    session = registry.startInstr(config.getEncoding());
                    final FileInfo fileInfo = session.enterFile(
                            pkg, srcFile, lastLineNumber, 0,
                            srcFile.lastModified(), srcFile.length(), calculateChecksum(srcFile)); // HACK - nclinecount

                    addRecorderIncCalls(sourceUnit, module, testSourceContext, flagsForInstrumentedClasses);
                    session.exitFile();

                    // second pass - add helper stuff like recorderInc method, elvis wrapper etc
                    addHelperFieldsAndMethods(fileInfo, flagsForInstrumentedClasses);

                    maybeDumpAST(module, sourceUnit, "Instrumented source", ".after.instruction.txt");

                    session.close();
                    registry.saveAndAppendToFile();
                }
            }
        } catch (Exception e) {
            final RuntimeException re = new RuntimeException("OpenClover Groovy integration failed to instrument Groovy source: " + getSourceUnitFile(sourceUnit), e);
            Logger.getInstance().error(re.getMessage(), re);
            throw re;
        }
    }

    protected void addRecorderIncCalls(final SourceUnit sourceUnit, final ModuleNode module,
                                       final TestDetector.SourceContext testSourceContext,
                                       final Map<ClassNode, GroovyInstrumentationResult> flagsForInstrumentedClasses) {
        final List<ClassNode> classes = module.getClasses();
        if (classes != null) {
            for (final ClassNode clazz : classes) {
                if (!GroovyUtils.isReportable(clazz)) {
                    // warn about non-instrumented class
                    Logger.getInstance().verbose("Class " + clazz.getName() + " cannot not instrumented because "
                            + "AST contains invalid source region definition ("
                            + clazz.getLineNumber() + ":" + clazz.getColumnNumber()
                            + " - " + clazz.getLastLineNumber() + ":" + clazz.getLastColumnNumber() + ")");
                    break;
                }

                // detect if we have a test class and of which kind
                final TestDetector.TypeContext typeContext = new GroovyClassTypeContext(clazz);
                final boolean isTestClass = config.getTestDetector().isTypeMatch(testSourceContext, typeContext);
                final boolean isSpock = SpockFeatureNameExtractor.isClassWithSpecAnnotations(typeContext.getModifiers());
                final boolean isJUnit = JUnitParameterizedTestExtractor.isParameterizedClass(typeContext.getModifiers());

                // perform instrumentation
                final InstrumentingCodeVisitor instrumenter = new InstrumentingCodeVisitor(
                        config, session, registry, sourceUnit,
                        testSourceContext, isTestClass, clazz);
                if (instrumenter.instrument(clazz)) {
                    // ... and store some flags for further class enhancements
                    flagsForInstrumentedClasses.put(
                            clazz,
                            new GroovyInstrumentationResult(
                                    instrumenter.isElvisExprUsed(),
                                    instrumenter.isFieldExprUsed(),
                                    instrumenter.isSafeExprUsed(),
                                    instrumenter.isTestResultsRecorded(),
                                    instrumenter.getSafeEvalMethods(), isTestClass, isSpock, isJUnit)
                    );
                }
            }
        }
    }

    /**
     * Based on flags from first instrumentation pass enhance instrumented classes by adding extra fields and methods,
     * such as:
     * <ul>
     *  <li>$CLV_R$ field and $CLV_R$() getter for CoverageRecorder</li>
     *  <li>elvis operator</li>
     *  <li>boolean expression</li>
     *  <li>safe evalation</li>
     *  <li>test result recording</li>
     * </ul>
     */
    protected void addHelperFieldsAndMethods(final FileInfo fileInfo,
                                             final Map<ClassNode, GroovyInstrumentationResult> flagsForInstrumentedClasses) {
        // check which classes have been instrumented and generate extra methods according to needs
        GroovyInstrumentationConfig sessionConfig = new GroovyInstrumentationConfig(
                config.getInitString(),
                config.getDistributedConfigString(),
                session.getVersion(),
                CoverageRecorder.getConfigBits(
                        config.getFlushPolicy(),
                        config.getFlushInterval(),
                        false, false, !config.isSliceRecording()),
                fileInfo.getDataIndex() + fileInfo.getDataLength(),
                config.getProfiles());

        for (Map.Entry<ClassNode, GroovyInstrumentationResult> entry : flagsForInstrumentedClasses.entrySet()) {
            ClassNode clazz = entry.getKey();
            GroovyInstrumentationResult flags = entry.getValue();

            fillRecorderGetterBytecode(clazz, sessionConfig);
            createSafeEvalMethods(clazz, flags);
            createEvalTestExceptionMethod(clazz, flags);
            createTestNameSnifferField(clazz, flags);
        }
    }

    private void fillRecorderGetterBytecode(ClassNode clazz, GroovyInstrumentationConfig sessionConfig) {
        final MethodNode recorderGetter = clazz.getMethod(recorderGetterName, new Parameter[0]);
        final BytecodeInstruction bytecodeInstruction = newRecorderGetterBytecodeInstruction(clazz, sessionConfig);
        ((BlockStatement) recorderGetter.getCode()).addStatement(new BytecodeSequence(bytecodeInstruction));
    }

    private void createSafeEvalMethods(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.safeExprUsed) {
            for (final MethodNode methodNode : flags.safeEvalMethods.values()) {
                clazz.addMethod(methodNode);
            }
        }
    }

    private void createEvalTestExceptionMethod(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.testResultsRecorded) {
            addEvalTestException(clazz);
        }
    }

    /**
     * Add the field named {@link CloverNames#CLOVER_TEST_NAME_SNIFFER} to the class.
     *
     * @param clazz class to be extended
     * @param flags flags after first instrumentation pass
     */
    private void createTestNameSnifferField(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.isTestClass) {
            createTestNameSnifferField(clazz,
                    flags.isSpockSpecification ? SnifferType.SPOCK
                            : (flags.isParameterizedJUnit ? SnifferType.JUNIT : SnifferType.NULL));
        }
    }

    /**
     * Add the field named {@link CloverNames#CLOVER_TEST_NAME_SNIFFER} to the class.
     *
     * @param clazz       class to be extended
     * @param snifferType type of the sniffer to be embedded
     */
    private void createTestNameSnifferField(final ClassNode clazz, final SnifferType snifferType) {
        final Expression fieldInitializationExpr;
        switch (snifferType) {
            case JUNIT:
            case SPOCK:
                // new TestNameSniffer.Simple()
                fieldInitializationExpr = new ConstructorCallExpression(
                        createSimpleSnifferClassNode(), ArgumentListExpression.EMPTY_ARGUMENTS);
                break;
            case NULL:
            default:
                // TestNameSniffer.NULL_INSTANCE
                fieldInitializationExpr = new PropertyExpression(
                        new ClassExpression(ClassHelper.make(TestNameSniffer.class)), "NULL_INSTANCE");
                break;
        }

        // add field
        // public static final __CLRx_y_z_TEST_NAME_SNIFFER = ...
        clazz.addField( CloverNames.CLOVER_TEST_NAME_SNIFFER,
                ACC_STATIC | ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL,
                        ClassHelper.make(org_openclover_runtime.TestNameSniffer.class),
                        fieldInitializationExpr);
    }

    /**
     * @return ClassNode representing TestNameSniffer.Simple class
     */
    private ClassNode createSimpleSnifferClassNode() {
        return ClassHelper.make(TestNameSniffer.Simple.class);
    }

    private void addEvalTestException(ClassNode clazz) {
        //int evalTestException(Throwable exception, def expected) {
        //  boolean isExpected = false
        //  for (ex in expected) {
        //      isExpected = isExpected || (exception != null && ex.isAssignableFrom(exception.getClass()))
        //  }
        //  return (isExpected || (exception == null && expected.isEmpty())) ? $PerTestRecorder.NORMAL_EXIT$ : $PerTestRecorder.ABNORMAL_EXIT$
        //}
        Parameter exceptionParam = new Parameter(ClassHelper.make(Throwable.class), "exception");
        Parameter expectedParam = new Parameter(ClassHelper.DYNAMIC_TYPE, "expected");
        VariableScope methodScope = new VariableScope();

        VariableExpression isExpectedVar = new VariableExpression("isExpected", ClassHelper.Boolean_TYPE);
        Parameter exVariable = new Parameter(ClassHelper.make(Class.class), "ex");

        clazz.addMethod(
                CloverNames.namespace("evalTestException"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.int_TYPE,
                new Parameter[]{exceptionParam, expectedParam},
                new ClassNode[]{},
                new BlockStatement(
                        new Statement[]{
                                new ExpressionStatement(
                                        new DeclarationExpression(
                                                isExpectedVar,
                                                Token.newSymbol(Types.EQUAL, -1, -1),
                                                ConstantExpression.FALSE
                                        )),
                                new ForStatement(
                                        exVariable,
                                        new VariableExpression(expectedParam),
                                        new BlockStatement(
                                                new Statement[]{
                                                        new ExpressionStatement(
                                                                new BinaryExpression(
                                                                        isExpectedVar,
                                                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                                                        new BinaryExpression(
                                                                                isExpectedVar,
                                                                                Token.newSymbol(Types.LOGICAL_OR, -1, -1),
                                                                                new BinaryExpression(
                                                                                        new BinaryExpression(
                                                                                                new VariableExpression(exceptionParam),
                                                                                                Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1),
                                                                                                ConstantExpression.NULL),
                                                                                        Token.newSymbol(Types.LOGICAL_AND, -1, -1),
                                                                                        new MethodCallExpression(
                                                                                                new VariableExpression(exVariable),
                                                                                                "isAssignableFrom",
                                                                                                new MethodCallExpression(
                                                                                                        new VariableExpression(exceptionParam),
                                                                                                        "getClass",
                                                                                                        ArgumentListExpression.EMPTY_ARGUMENTS))))))
                                                },
                                                methodScope
                                        )),
                                new ReturnStatement(
                                        new TernaryExpression(
                                                new BooleanExpression(
                                                        new BinaryExpression(
                                                                isExpectedVar,
                                                                Token.newSymbol(Types.LOGICAL_OR, -1, -1),
                                                                new BinaryExpression(
                                                                        new BinaryExpression(
                                                                                new VariableExpression(exceptionParam),
                                                                                Token.newSymbol(Types.COMPARE_EQUAL, -1, -1),
                                                                                ConstantExpression.NULL),
                                                                        Token.newSymbol(Types.LOGICAL_AND, -1, -1),
                                                                        new MethodCallExpression(
                                                                                new VariableExpression(expectedParam),
                                                                                "isEmpty",
                                                                                ArgumentListExpression.EMPTY_ARGUMENTS)))),
                                                new ConstantExpression(PerTestRecorder.NORMAL_EXIT),
                                                new ConstantExpression(PerTestRecorder.ABNORMAL_EXIT)
                                        ))
                        },
                        methodScope));
    }

}