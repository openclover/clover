package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.instr.tests.naming.JUnitParameterizedTestExtractor;
import org.openclover.core.instr.tests.naming.SpockFeatureNameExtractor;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org_openclover_runtime.CoverageRecorder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static org.openclover.core.util.Maps.newHashMap;

/**
 * We attach to the instruction selection phase because
 * most ast transformers occur in the canonicalization phase
 */
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class CloverAstTransformerInstructionSelection extends CloverAstTransformerBase {

    private final ClassNodeSafeEvalMethodsTransformer safeEvalMethodsTransformer;
    private final ClassNodeElvisEvalTransformer elvisEvalTransformer;
    private final ClassNodeExprEvalTransformer exprEvalTransformer;
    private final ClassNodeTestSnifferTransformer testSnifferTransformer;
    private final ClassNodeEvalTestExceptionTransformer testExceptionTransformer;

    /** See clover-groovy/src/main/assembly/META-INF/services/org.codehaus.groovy.transform.ASTTransformation */
    @SuppressWarnings("unused")
    public CloverAstTransformerInstructionSelection() throws IOException, CloverException {
        super(newConfigFromResource());

        final Function<ClassRowColumn, Expression> newRecorder =
                classRowColumn -> newRecorderExpression(classRowColumn.classRef, classRowColumn.row, classRowColumn.column);

        safeEvalMethodsTransformer = new ClassNodeSafeEvalMethodsTransformer();
        elvisEvalTransformer = new ClassNodeElvisEvalTransformer(newRecorder);
        exprEvalTransformer = new ClassNodeExprEvalTransformer(newRecorder);
        testSnifferTransformer = new ClassNodeTestSnifferTransformer();
        testExceptionTransformer = new ClassNodeEvalTestExceptionTransformer();
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
            // Companion classes for interfaces are collected separately to avoid ConcurrentModificationException.
            final List<ClassNode> companionClasses = new ArrayList<>();
            // Fully-qualified names of interfaces whose default methods are already handled via $Trait$Helper.
            final Set<String> traitBackedInterfaces = new HashSet<>();

            for (final ClassNode clazz : classes) {
                if (!GroovyUtils.isReportable(clazz)) {
                    // Groovy generates a synthetic $Trait$Helper class (with invalid source regions)
                    // that holds the actual implementations of default interface methods.
                    // Instrument those methods and register them under the parent interface's name.
                    if (isTraitHelper(clazz)) {
                        final String parentFullName = traitHelperParentName(clazz);
                        final String parentSimpleName = simpleNameOf(parentFullName);
                        final InstrumentingCodeVisitor instrumenter = new InstrumentingCodeVisitor(
                                config, session, registry, sourceUnit,
                                testSourceContext, false, clazz, parentSimpleName);
                        if (instrumenter.instrument(clazz)) {
                            flagsForInstrumentedClasses.put(clazz, new GroovyInstrumentationResult(
                                    instrumenter.isElvisExprUsed(), instrumenter.isFieldExprUsed(),
                                    instrumenter.isSafeExprUsed(), instrumenter.isTestResultsRecorded(),
                                    instrumenter.getSafeEvalMethods(), false, false, false));
                        }
                        traitBackedInterfaces.add(parentFullName);
                    } else {
                        Logger.getInstance().verbose("Class " + clazz.getName() + " cannot not instrumented because "
                                + "AST contains invalid source region definition ("
                                + clazz.getLineNumber() + ":" + clazz.getColumnNumber()
                                + " - " + clazz.getLastLineNumber() + ":" + clazz.getLastColumnNumber() + ")");
                    }
                    continue;
                }

                // Skip interfaces that have already been handled via their $Trait$Helper.
                if (clazz.isInterface() && traitBackedInterfaces.contains(clazz.getName())) {
                    continue;
                }

                // detect if we have a test class and of which kind
                final TestDetector.TypeContext typeContext = new GroovyClassTypeContext(clazz);
                final boolean isTestClass = config.getTestDetector().isTypeMatch(testSourceContext, typeContext);
                final boolean isSpock = SpockFeatureNameExtractor.isClassWithSpecAnnotations(typeContext.getModifiers());
                final boolean isJUnit = JUnitParameterizedTestExtractor.isParameterizedClass(typeContext.getModifiers());

                // For remaining interfaces (without $Trait$Helper), use a synthetic companion class
                // as the recorder target since interface fields cannot be private/mutable on Java 8.
                final ClassNode classRef;
                if (clazz.isInterface()) {
                    classRef = createCompanionClass(clazz);
                    companionClasses.add(classRef);
                } else {
                    classRef = clazz;
                }

                // perform instrumentation
                final InstrumentingCodeVisitor instrumenter = new InstrumentingCodeVisitor(
                        config, session, registry, sourceUnit,
                        testSourceContext, isTestClass, classRef);
                if (instrumenter.instrument(clazz)) {
                    // ... and store some flags for further class enhancements
                    // For interfaces, enhancements go to the companion class (not the interface itself).
                    flagsForInstrumentedClasses.put(
                            classRef,
                            new GroovyInstrumentationResult(
                                    instrumenter.isElvisExprUsed(),
                                    instrumenter.isFieldExprUsed(),
                                    instrumenter.isSafeExprUsed(),
                                    instrumenter.isTestResultsRecorded(),
                                    instrumenter.getSafeEvalMethods(), isTestClass, isSpock, isJUnit)
                    );
                }
            }

            // Add companion classes to the module so Groovy compiles them alongside the interfaces.
            for (final ClassNode companion : companionClasses) {
                module.addClass(companion);
            }
        }
    }

    /**
     * Creates a synthetic companion class to hold the Clover recorder for an interface.
     * The companion is named {@code InterfaceName$__clover$} and lives in the same package.
     */
    private ClassNode createCompanionClass(final ClassNode interfaceNode) {
        return new ClassNode(interfaceNode.getName() + "$__clover$", ACC_PUBLIC, ClassHelper.OBJECT_TYPE);
    }

    /** Returns true if the class is a Groovy-generated trait helper (e.g. {@code Foo$Trait$Helper}). */
    private boolean isTraitHelper(final ClassNode clazz) {
        return clazz.getName().contains("$Trait$Helper");
    }

    /** Derives the fully-qualified parent interface name from a {@code $Trait$Helper} class name. */
    private String traitHelperParentName(final ClassNode helper) {
        final String name = helper.getName();
        return name.substring(0, name.lastIndexOf("$Trait$Helper"));
    }

    /** Returns the simple (unqualified) name of a fully-qualified class name. */
    private String simpleNameOf(final String fqName) {
        final int dot = fqName.lastIndexOf('.');
        return dot >= 0 ? fqName.substring(dot + 1) : fqName;
    }

    /**
     * Based on flags from first instrumentation pass enhance instrumented classes by adding extra fields and methods,
     * such as:
     * <ul>
     *  <li>$CLV_R$ field and $CLV_R$() getter for CoverageRecorder</li>
     *  <li>elvis operator</li>
     *  <li>boolean expression</li>
     *  <li>safe evaluation</li>
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

        final ClassNodeRecorderTransformer recorderTransformer = new ClassNodeRecorderTransformer(sessionConfig, recorderFieldName, recorderGetterName);

        for (Map.Entry<ClassNode, GroovyInstrumentationResult> entry : flagsForInstrumentedClasses.entrySet()) {
            ClassNode clazz = entry.getKey();
            GroovyInstrumentationResult flags = entry.getValue();

            recorderTransformer.transform(clazz, flags);
            safeEvalMethodsTransformer.transform(clazz, flags);
            testExceptionTransformer.transform(clazz, flags);
            testSnifferTransformer.transform(clazz, flags);
            elvisEvalTransformer.transform(clazz, flags);
            exprEvalTransformer.transform(clazz, flags);
        }
    }

}