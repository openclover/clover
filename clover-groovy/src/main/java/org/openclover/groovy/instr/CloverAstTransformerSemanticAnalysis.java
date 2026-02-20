package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;

import java.io.IOException;

import static groovyjarjarasm.asm.Opcodes.ACC_PRIVATE;
import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;
import static groovyjarjarasm.asm.Opcodes.ACC_SYNTHETIC;

/**
 * We attach to the semantic selection phase primarily because we want to allow Groovy
 * to resolve our classes.
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class CloverAstTransformerSemanticAnalysis extends CloverAstTransformerBase {

    /** See clover-groovy/src/main/assembly/META-INF/services/org.codehaus.groovy.transform.ASTTransformation */
    public CloverAstTransformerSemanticAnalysis() throws IOException, CloverException {
        super(newConfigFromResource());
    }

    @Override
    public void visit(SourceUnit sourceUnit) {
        try {
            final ModuleNode module = sourceUnit.getAST();
            if (config != null && config.isEnabled() && !hasRecorderField(module)) {
                if (!isIncluded(sourceUnit)) {
                    Logger.getInstance().verbose("Skipping " + getSourceUnitFile(sourceUnit));
                } else {

                    maybeDumpAST(module, sourceUnit, "Original source", ".before.semantic.txt");

                    // add helper stuff like recorderInc method, elvis wrapper etc
                    for (ClassNode classNode : module.getClasses()) {
                        addHelperFieldsAndMethods(classNode);
                    }

                    maybeDumpAST(module, sourceUnit, "Instrumented source", ".after.semantic.txt");
                }
            }
        } catch (Exception e) {
            final RuntimeException re = new RuntimeException("OpenClover Groovy integration failed to instrument Groovy source: " + getSourceUnitFile(sourceUnit), e);
            Logger.getInstance().error(re.getMessage(), re);
            throw re;
        }
    }

    /**
     * Protect against double instrumentation of the same source file. Simply check for presence of
     * the recorder field in at least one class in the source file.
     *
     * @return true if at least one class in the module has the recorder field
     */
    private boolean hasRecorderField(ModuleNode module) {
        return module.getClasses()
                .stream()
                .flatMap(clazz -> clazz.getFields().stream())
                .map(FieldNode::getName)
                .anyMatch(name -> name.equals(recorderFieldName));
    }

    /**
     * Creates a static field for CoverageRecorder and a static method (lazy initialization) like:
     * <pre>
     *     private static CoverageRecorder $CLV_R$ = null;
     *     private static CoverageRecorder $CLV_R$() {
     *          ...
     *     }
     * </pre>
     */
    private void createRecorderFieldAndGetter(final ClassNode clazz) {
        // add field
        clazz.addField(recorderFieldName, ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC,
                ClassHelper.make(org_openclover_runtime.CoverageRecorder.class),
                ConstantExpression.NULL);

        // add method (no code yet)
        clazz.addMethod(recorderGetterName, ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC,
                ClassHelper.make(org_openclover_runtime.CoverageRecorder.class),
                new Parameter[]{}, new ClassNode[]{},
                new BlockStatement());

        // getter method will be filled with byte code in next stage, see CloverAstTransformerInstructionSelection
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
    protected void addHelperFieldsAndMethods(final ClassNode clazz) {
        // check which classes have been instrumented and generate extra methods according to needs
        createRecorderFieldAndGetter(clazz);
        createEvalElvisMethods(clazz);
        createExprEvalMethod(clazz);
    }

    private void createEvalElvisMethods(final ClassNode clazz) {
        addEvalElvisPrimitive(clazz, ClassHelper.byte_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.short_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.int_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.long_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.float_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.double_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.boolean_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.char_TYPE);
        addEvalElvisDef(clazz);
    }

    private void createExprEvalMethod(final ClassNode clazz) {
        addExprEvalDef(clazz);
    }

    private void addExprEvalDef(ClassNode clazz) {
        //def exprEval(def expr, Integer index) {
        //  RECORDERCLASS.R.inc(index)
        //  return expr
        //}
        final Parameter expr = new Parameter(ClassHelper.DYNAMIC_TYPE, "expr");
        final Parameter index = new Parameter(ClassHelper.Integer_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        newRecorderExpression(clazz, -1, -1),
                                        "inc",
                                        new ArgumentListExpression(new VariableExpression(index)))),
                        new ReturnStatement(new VariableExpression(expr))
                },
                methodScope);

        clazz.addMethod(
                CloverNames.namespace("exprEval"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.DYNAMIC_TYPE,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);
    }

    private void addEvalElvisPrimitive(ClassNode clazz, ClassNode primitiveType) {
        //T = boolean|byte|char|short|int|long|float|double
        //T elvisEval(T expr, int index) {
        //  boolean isTrue = expr as Boolean
        //  if (isTrue) { RECORDER_CLASS.R.inc(index) } else { RECORDER_CLASS.R.inc(index + 1) }
        //  return expr
        //}

        final Parameter expr = new Parameter(primitiveType, "expr");
        final Parameter index = new Parameter(ClassHelper.int_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new DeclarationExpression(
                                        new VariableExpression("isTrue", ClassHelper.Boolean_TYPE),
                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                        CastExpression.asExpression(ClassHelper.Boolean_TYPE, new VariableExpression(expr)))),
                        new IfStatement(
                                new BooleanExpression(new VariableExpression("isTrue", ClassHelper.Boolean_TYPE)),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(new VariableExpression(index)))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(
                                                                new BinaryExpression(
                                                                        new VariableExpression(index),
                                                                        Token.newSymbol(Types.PLUS, -1, -1),
                                                                        new ConstantExpression(1))))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope))

                },
                methodScope
        );

        final MethodNode methodNode = new MethodNode(CloverNames.namespace("elvisEval"),
                ACC_STATIC | ACC_PUBLIC,
                primitiveType,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);

        clazz.addMethod(methodNode);
    }

    private void addEvalElvisDef(ClassNode clazz) {
        //def elvisEval(def expr, Integer index) {
        //  boolean isTrue = expr as Boolean
        //  if (isTrue) { RECORDERCLASS.R.inc(index) } else { RECORDERCLASS.R.inc(index + 1) }
        //  return expr
        //}
        final Parameter expr = new Parameter(ClassHelper.DYNAMIC_TYPE, "expr");
        final Parameter index = new Parameter(ClassHelper.Integer_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new DeclarationExpression(
                                        new VariableExpression("isTrue", ClassHelper.Boolean_TYPE),
                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                        CastExpression.asExpression(ClassHelper.Boolean_TYPE, new VariableExpression(expr)))),
                        new IfStatement(
                                new BooleanExpression(new VariableExpression("isTrue", ClassHelper.Boolean_TYPE)),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(new VariableExpression(index)))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(
                                                                new BinaryExpression(
                                                                        new VariableExpression(index),
                                                                        Token.newSymbol(Types.PLUS, -1, -1),
                                                                        new ConstantExpression(1))))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope))

                },
                methodScope
        );

        clazz.addMethod(
                CloverNames.namespace("elvisEval"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.DYNAMIC_TYPE,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);
    }

}