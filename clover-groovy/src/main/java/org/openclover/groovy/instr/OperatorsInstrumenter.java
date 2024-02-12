package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.core.util.collections.Pair;
import org.openclover.runtime.CloverNames;

import java.util.Map;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;
import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newHashMap;

/**
 * Instruments Groovy operators like:
 *  - elvis (a ?: b)
 *  - null-safe navigation (a ?. b)
 *  - ternary expression (a ? b : c)
 */
public class OperatorsInstrumenter extends ClassInstumenter {

    private final BranchInstrumenter branchInstrumenter;

    // list of wrapper methods "T saveEval_T(T expr, int)" for "?." operator
    @NotNull
    private Map<String, MethodNode> safeEvalMethods = newHashMap();

    public OperatorsInstrumenter(@NotNull final InstrumentationSession session,
                                 @NotNull final ClassNode classRef,
                                 @NotNull final BranchInstrumenter branchInstrumenter) {
        super(session, classRef);
        this.branchInstrumenter = branchInstrumenter;
    }

    @NotNull
    public Map<String, MethodNode> getSafeEvalMethods() {
        return safeEvalMethods;
    }

    @NotNull
    public Pair<ElvisOperatorExpression, Boolean> transformElvis(
            @NotNull final ElvisOperatorExpression elvis,
            @NotNull final ContextSet currentMethodContext) {

        final SourceInfo srcRegion = countExpressionRegion(elvis.getTrueExpression());

        if (srcRegion != null) {
            final BranchInfo branch = session.addBranch(currentMethodContext, srcRegion, true,
                    1 + ExpressionComplexityCounter.count(elvis),
                    LanguageConstruct.Builtin.GROOVY_ELVIS_OPERATOR);
            return Pair.of(
                    new ElvisOperatorExpression(
                            new StaticMethodCallExpression(
                                    classRef,
                                    CloverNames.namespace("elvisEval"),
                                    new ArgumentListExpression(
                                            elvis.getTrueExpression(),
                                            new ConstantExpression(branch.getDataIndex()))),
                            elvis.getFalseExpression()),
                    true);
        }

        return Pair.of(elvis, false);
    }

    @NotNull
    public Pair<AttributeExpression, Boolean> transformSafeAttributeExpression(
            @NotNull final AttributeExpression attributeExpression,
            @NotNull final ClassNode currentClassNode,
            @NotNull final ContextSet currentMethodContext) {

        final SourceInfo srcRegion = countExpressionRegion(attributeExpression.getObjectExpression());
        if (srcRegion != null) {
            // add safeEval_com_acme_Foo(Foo, int) method to the class
            createSafeEvalMethodIfNecessary(attributeExpression.getObjectExpression().getType(), currentClassNode);

            // transform "foo?.bar" to "safeEval_com_acme_Foo(foo, idx)?.bar"
            attributeExpression.setObjectExpression(
                    newSafeEval(
                            session.addBranch(
                                    currentMethodContext,
                                    srcRegion,
                                    true,
                                    1 + ExpressionComplexityCounter.count(attributeExpression.getObjectExpression()),
                                    LanguageConstruct.Builtin.GROOVY_SAFE_ATTRIBUTE),
                            attributeExpression.getObjectExpression()));

            return Pair.of(attributeExpression, true);
        }

        return Pair.of(attributeExpression, false);
    }

    @NotNull
    public Pair<MethodCallExpression, Boolean> transformSafeMethodCall(
            @NotNull final MethodCallExpression methodCallExpression,
            @NotNull final ClassNode currentClassNode,
            @NotNull final ContextSet currentMethodContext) {

        final SourceInfo srcRegion = countExpressionRegion(methodCallExpression.getObjectExpression());
        if (srcRegion != null) {
            // add safeEval_com_acme_Foo(Foo, int) method to the class
            createSafeEvalMethodIfNecessary(methodCallExpression.getObjectExpression().getType(), currentClassNode);

            // transform "foo?.bar" to "safeEval_com_acme_Foo(foo, idx)?.bar"
            methodCallExpression.setObjectExpression(
                    newSafeEval(
                            session.addBranch(
                                    currentMethodContext,
                                    srcRegion,
                                    true,
                                    1 + ExpressionComplexityCounter.count(methodCallExpression.getObjectExpression()),
                                    LanguageConstruct.Builtin.GROOVY_SAFE_METHOD),
                            methodCallExpression.getObjectExpression()));
            return Pair.of(methodCallExpression, true);
        }

        return Pair.of(methodCallExpression, false);
    }

    @NotNull
    public Pair<PropertyExpression, Boolean> transformSafeProperty(
            @NotNull final PropertyExpression propertyExpression,
            @NotNull final ClassNode currentClassNode,
            @NotNull final ContextSet currentMethodContext) {

        final SourceInfo srcRegion = countExpressionRegion(propertyExpression.getObjectExpression());
        if (srcRegion != null) {
            // add safeEval_com_acme_Foo(Foo, int) method to the class
            createSafeEvalMethodIfNecessary(propertyExpression.getObjectExpression().getType(), currentClassNode);

            // transform "foo?.bar" to "safeEval_com_acme_Foo(foo, idx)?.bar"
            propertyExpression.setObjectExpression(
                    newSafeEval(
                            session.addBranch(
                                    currentMethodContext,
                                    srcRegion,
                                    true,
                                    1 + ExpressionComplexityCounter.count(propertyExpression.getObjectExpression()),
                                    LanguageConstruct.Builtin.GROOVY_SAFE_PROPERTY),
                            propertyExpression.getObjectExpression()));
            return Pair.of(propertyExpression, true);
        }
        return Pair.of(propertyExpression, false);
    }

    @NotNull
    public TernaryExpression transformTernary(
            @NotNull final TernaryExpression ternary,
            @NotNull final ContextSet currentMethodContext) {

        return new TernaryExpression(
                branchInstrumenter.transformBranch(
                        countExpressionRegion(ternary.getBooleanExpression()),
                        ternary.getBooleanExpression(),
                        currentMethodContext),
                ternary.getTrueExpression(),
                ternary.getFalseExpression());
    }

    @NotNull
    protected String sanitizeClassName(@NotNull final ClassNode clazz) {
        return clazz.getName().replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * e.g. $CLV_safeEval_java_lang_String$
     */
    @NotNull
    protected String getSafeEvalMethodName(@NotNull final ClassNode classNode) {
        return CloverNames.namespace("safeEval_" + sanitizeClassName(classNode));
    }

    /**
     * Create safeEval_X() method with a proper type signature if it does not exist and add to hashmap.
     * @param evalReturnType what type eval expression has so that our wrapper should return the same type
     */
    protected void createSafeEvalMethodIfNecessary(
            @NotNull final ClassNode evalReturnType,
            @NotNull final ClassNode currentClassNode) {
        final String safeEvalMethodName = getSafeEvalMethodName(evalReturnType);
        if (!safeEvalMethods.containsKey(safeEvalMethodName)) {
            safeEvalMethods.put(
                    safeEvalMethodName,
                    createSafeEvalMethod(currentClassNode, evalReturnType, safeEvalMethodName));
        }
    }

    @NotNull
    protected MethodNode createSafeEvalMethod(
            @NotNull final ClassNode clazz,
            @NotNull final ClassNode evalReturnType,
            @NotNull final String safeEvalMethodName) {
        // note: it would be great to have a such method signature:
        //    public static <T> T safeEval(T expr, Integer index)
        // but groovyc fails to perform type matching when an expression with safe navigation operator is used with our
        // generic safeEval() method, e.g. "A a = safeEval(b, 123)?.a"
        // thus we simulate genericity by making a number of copies of safeEval methods - one for each type T

        // declare (T expr, Integer index) parameters
        final Parameter expr = new Parameter(evalReturnType, "expr");
        final Parameter index = new Parameter(ClassHelper.Integer_TYPE, "index");

        //public static T safeEval_T(T expr, Integer index)
        //  boolean notNull = expr != null
        //  if (notNull) { R().inc(index) } else { R().inc(index + 1) }
        //  RECORDERCLASS.R.inc(index)
        //  return expr
        //}
        final VariableScope methodScope = new VariableScope();
        final Statement methodsCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new DeclarationExpression(
                                        new VariableExpression("notNull", ClassHelper.Boolean_TYPE),
                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                        new BooleanExpression(
                                                new BinaryExpression(
                                                        new VariableExpression(expr),
                                                        Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1),
                                                        ConstantExpression.NULL)))),
                        new IfStatement(
                                new BooleanExpression(new VariableExpression("notNull", ClassHelper.Boolean_TYPE)),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        Grover.newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(new VariableExpression(index))))
                                }, methodScope),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        Grover.newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(
                                                                new BinaryExpression(
                                                                        new VariableExpression(index),
                                                                        Token.newSymbol(Types.PLUS, -1, -1),
                                                                        new ConstantExpression(1))))),
                                }, methodScope)),
                        new ReturnStatement(new VariableExpression(expr))
                },
                methodScope);

        final MethodNode methodNode = new MethodNode(
                safeEvalMethodName,
                ACC_STATIC | ACC_PUBLIC,
                evalReturnType,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodsCode);
        methodNode.setGenericsTypes(evalReturnType.getGenericsTypes());
        return methodNode;
    }

    @NotNull
    protected StaticMethodCallExpression newSafeEval(
            @NotNull final BranchInfo branch,
            @NotNull final Expression objectExpression) {
        return new StaticMethodCallExpression(
                classRef,
                getSafeEvalMethodName(objectExpression.getType()),
                new ArgumentListExpression(
                        newArrayList(
                                objectExpression,
                                new ConstantExpression(branch.getDataIndex())
                        )));
    }

}
