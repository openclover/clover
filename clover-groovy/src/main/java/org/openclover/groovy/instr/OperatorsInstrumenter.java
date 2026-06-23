package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
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
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.core.util.collections.Pair;
import org.openclover.runtime.CloverNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;
import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.groovy.instr.CloverAstTransformerBase.newRecorderExpression;

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
    public Pair<ClosureExpression, Boolean> transformLambda(
            @NotNull final ClosureExpression lambdaExpression,
            @NotNull final ClassNode currentClassNode,
            @NotNull final ContextSet currentMethodContext) {
        // Lambda body statements are already instrumented by InstrumentingCodeVisitor.visitClosureExpression().
        // In Groovy 3, LambdaExpression.visit() dispatches to visitLambdaExpression() which falls back to
        // visitClosureExpression() via CodeVisitorSupport, so instrumentBlockStatement() is applied to the body.
        // No additional transformation is needed here to avoid double-instrumentation.
        // Note: parameter type is ClosureExpression (parent of LambdaExpression) to avoid a static reference
        // to LambdaExpression, which does not exist in Groovy 2.x and would cause NoClassDefFoundError.
        return Pair.of(lambdaExpression, false);
    }

    // StaticTypesMarker.CLOSURE_ARGUMENTS is a Groovy 3+ enum constant set by the static type checker
    // on MethodReferenceExpression nodes when @CompileStatic is active. It carries the exact parameter
    // ClassNode[] the generated closure must accept (resolved from the target SAM type).
    // We load it via reflection to avoid a hard compile-time dependency on Groovy 3 in this module.
    private static final Object CLOSURE_ARGUMENTS_KEY = resolveClosureArgumentsKey();

    private static Object resolveClosureArgumentsKey() {
        try {
            final Class<?> markerClass = Class.forName("org.codehaus.groovy.transform.stc.StaticTypesMarker");
            for (final Object constant : markerClass.getEnumConstants()) {
                if ("CLOSURE_ARGUMENTS".equals(((Enum<?>) constant).name())) {
                    return constant;
                }
            }
        } catch (final Exception ignored) {}
        return null;
    }

    @NotNull
    public Pair<Expression, Boolean> transformMethodReference(
            @NotNull final Expression methodReference,
            @NotNull final ClassNode currentClassNode,
            @NotNull final ContextSet currentMethodContext) {
        // Note: parameter type is Expression (parent of MethodReferenceExpression) to avoid a static reference
        // to MethodReferenceExpression, which does not exist in Groovy 2.x and would cause NoClassDefFoundError.
        //
        // We must track each method reference separately: the enclosing statement may be reached while the
        // method reference is not (e.g. empty stream → map body never invoked).
        //
        // MethodReferenceExpression has no body to inject into, so we replace it with a Groovy closure that:
        //   (a) increments the hit counter on each invocation, and
        //   (b) delegates to the original method or constructor.
        //
        // Two strategies, selected by whether @CompileStatic resolved the SAM type:
        //
        //   @CompileStatic (CLOSURE_ARGUMENTS present): build a typed closure whose parameter types come
        //   directly from the resolved SAM — correct for overloaded instance/static/constructor refs.
        //
        //   Dynamic (CLOSURE_ARGUMENTS absent): build an untyped Object[] closure that recreates the
        //   method pointer via the Groovy '&' operator and delegates via MethodClosure.call(Object[]).
        //   Works for all ref kinds including constructors (Foo.&"new" produces a valid MethodClosure).
        //
        // Bound references (e.g. "hello"::length) have a non-ClassExpression receiver — we skip them
        // because the receiver is an arbitrary runtime expression we cannot safely reproduce in the closure.

        if (!(methodReference instanceof MethodPointerExpression)) {
            return Pair.of(methodReference, false);
        }

        final MethodPointerExpression methodPointer = (MethodPointerExpression) methodReference;
        final Expression receiverExpr = methodPointer.getExpression();
        final Expression methodNameExpr = methodPointer.getMethodName();

        // Only instrument unbound class-based refs: ReceiverClass::methodName
        if (!(receiverExpr instanceof ClassExpression) || !(methodNameExpr instanceof ConstantExpression)) {
            return Pair.of(methodReference, false);
        }

        final SourceInfo srcRegion = countExpressionRegion(methodReference);
        if (srcRegion == null) {
            return Pair.of(methodReference, false);
        }

        final ClassNode receiverClass = receiverExpr.getType();
        final String methodName = ((ConstantExpression) methodNameExpr).getValue().toString();
        final FullStatementInfo statementInfo = session.addStatement(
                currentMethodContext, srcRegion, 0, LanguageConstruct.Builtin.STATEMENT);

        final ClassNode[] closureArgs = getClosureArguments(methodReference);
        if (closureArgs != null) {
            return Pair.of(buildTypedClosure(receiverClass, methodName, closureArgs, statementInfo), true);
        } else {
            return Pair.of(buildDynamicClosure(receiverClass, methodName, statementInfo), true);
        }
    }

    /**
     * Returns the {@code CLOSURE_ARGUMENTS} metadata set by Groovy's static type checker on a
     * {@code MethodReferenceExpression} node, or {@code null} when running without {@code @CompileStatic}.
     */
    @Nullable
    private static ClassNode[] getClosureArguments(@NotNull final Expression methodRef) {
        if (CLOSURE_ARGUMENTS_KEY == null) {
            return null;
        }
        final Object value = methodRef.getNodeMetaData(CLOSURE_ARGUMENTS_KEY);
        return value instanceof ClassNode[] ? (ClassNode[]) value : null;
    }

    /**
     * Builds a typed closure for {@code @CompileStatic} method references using the exact parameter
     * types from the SAM interface resolved by the static type checker.
     *
     * <p>Handles all reference kinds:
     * <ul>
     *   <li>Unbound instance: {@code Integer::toString} with {@code closureArgs=[Integer]} →
     *       {@code { Integer $CLV_p0$ -> R.inc(N); return $CLV_p0$.toString() }}</li>
     *   <li>Static: {@code Integer::toBinaryString} with {@code closureArgs=[Integer]} →
     *       {@code { Integer $CLV_p0$ -> R.inc(N); return Integer.toBinaryString($CLV_p0$) }}</li>
     *   <li>Multi-param instance: {@code String::concat} with {@code closureArgs=[String,String]} →
     *       {@code { String $CLV_p0$, String $CLV_p1$ -> R.inc(N); return $CLV_p0$.concat($CLV_p1$) }}</li>
     *   <li>Class constructor: {@code Random::new} with {@code closureArgs=[]} →
     *       {@code { -> R.inc(N); return new Random() }}</li>
     *   <li>Array constructor: {@code String[]::new} with {@code closureArgs=[int]} →
     *       {@code { int $CLV_p0$ -> R.inc(N); return new String[$CLV_p0$] }}</li>
     * </ul>
     */
    @NotNull
    private ClosureExpression buildTypedClosure(
            @NotNull final ClassNode receiverClass,
            @NotNull final String methodName,
            @NotNull final ClassNode[] closureArgs,
            @NotNull final FullStatementInfo statementInfo) {

        final ExpressionStatement incStmt = makeIncStatement(statementInfo);
        final Parameter[] params = buildParams(closureArgs);
        final Expression delegate;

        if ("new".equals(methodName)) {
            if (receiverClass.isArray()) {
                // String[]::new → { int $CLV_p0$ -> R.inc(N); return new String[$CLV_p0$] }
                delegate = new ArrayExpression(
                        receiverClass.getComponentType(),
                        null,
                        Collections.singletonList(new VariableExpression(params[0])));
            } else {
                // Random::new → { [$CLV_p0$, ...] -> R.inc(N); return new Random([$CLV_p0$, ...]) }
                delegate = new ConstructorCallExpression(
                        receiverClass, new ArgumentListExpression(asArgs(params)));
            }
        } else if (isUnboundInstanceRef(receiverClass, methodName, closureArgs)) {
            // params[0] is the receiver instance; params[1..] are method arguments
            final Parameter[] methodArgs = Arrays.copyOfRange(params, 1, params.length);
            delegate = new MethodCallExpression(
                    new VariableExpression(params[0]),
                    methodName,
                    new ArgumentListExpression(asArgs(methodArgs)));
        } else {
            // Static ref: all params are method arguments
            delegate = new StaticMethodCallExpression(
                    receiverClass, methodName, new ArgumentListExpression(asArgs(params)));
        }

        return makeClosure(params, incStmt, delegate);
    }

    /**
     * Returns {@code true} when {@code receiverClass::methodName} is an unbound instance reference,
     * i.e. a non-static method on {@code receiverClass} exists with {@code closureArgs.length - 1}
     * parameters (the SAM receives the instance as its first argument).
     */
    private static boolean isUnboundInstanceRef(
            @NotNull final ClassNode receiverClass,
            @NotNull final String methodName,
            @NotNull final ClassNode[] closureArgs) {
        if (closureArgs.length == 0) {
            return false;
        }
        final int methodParamCount = closureArgs.length - 1;
        return receiverClass.getMethods(methodName).stream()
                .anyMatch(m -> !m.isStatic() && m.getParameters().length == methodParamCount);
    }

    /**
     * Builds an untyped {@code Object[]} closure for dynamic (non-{@code @CompileStatic}) method
     * references. Delegates via a recreated {@code MethodPointerExpression} ({@code &} operator),
     * which Groovy resolves at runtime to the correct overload.
     *
     * <p>Works for all reference kinds including constructors — {@code Foo.&"new"} produces a
     * {@code MethodClosure} that calls {@code new Foo()} when invoked.
     *
     * <pre>
     *   Integer::toString   →  { Object[] $CLV_args$ -> R.inc(N); (Integer.&toString).call($CLV_args$) }
     *   Integer::toBinaryString → same pattern, static dispatch handled by MethodClosure at runtime
     *   Random::new         →  { Object[] $CLV_args$ -> R.inc(N); (Random.&"new").call($CLV_args$) }
     *   String[]::new       →  { Object[] $CLV_args$ -> R.inc(N); (String[].&"new").call($CLV_args$) }
     * </pre>
     */
    @NotNull
    private ClosureExpression buildDynamicClosure(
            @NotNull final ClassNode receiverClass,
            @NotNull final String methodName,
            @NotNull final FullStatementInfo statementInfo) {

        final Parameter argsParam = new Parameter(ClassHelper.OBJECT_TYPE.makeArray(), "$CLV_args$");
        // Recreate the method pointer inside the closure body — no variable capture needed.
        final MethodPointerExpression methodPtr = new MethodPointerExpression(
                new ClassExpression(receiverClass), new ConstantExpression(methodName));
        final Expression delegate = new MethodCallExpression(
                methodPtr, "call", new ArgumentListExpression(new VariableExpression(argsParam)));
        return makeClosure(new Parameter[]{ argsParam }, makeIncStatement(statementInfo), delegate);
    }

    /** Builds closure {@link Parameter}s from the {@code ClassNode[]} types in {@code closureArgs}. */
    @NotNull
    private static Parameter[] buildParams(@NotNull final ClassNode[] closureArgs) {
        final Parameter[] params = new Parameter[closureArgs.length];
        for (int i = 0; i < closureArgs.length; i++) {
            params[i] = new Parameter(closureArgs[i], "$CLV_p" + i + "$");
        }
        return params;
    }

    /**
     * Creates a {@code ClosureExpression} with a fresh {@code VariableScope} whose body is:
     * <pre>
     *   incStmt          // R.inc(statementIndex)
     *   return delegate  // the actual method / constructor call
     * </pre>
     */
    @NotNull
    private ClosureExpression makeClosure(
            @NotNull final Parameter[] params,
            @NotNull final ExpressionStatement incStmt,
            @NotNull final Expression delegateCall) {

        final VariableScope scope = new VariableScope();
        final ClosureExpression closure = new ClosureExpression(
                params,
                new BlockStatement(
                        new Statement[]{ incStmt, new ReturnStatement(delegateCall) },
                        new VariableScope(scope)));
        closure.setVariableScope(scope);
        return closure;
    }

    /** Converts an array of closure parameters to a list of {@link VariableExpression}s for a call. */
    @NotNull
    private static List<Expression> asArgs(@NotNull final Parameter[] params) {
        final List<Expression> args = new ArrayList<>(params.length);
        for (final Parameter p : params) {
            args.add(new VariableExpression(p));
        }
        return args;
    }

    /** Creates the {@code R.inc(statementIndex)} expression statement. */
    @NotNull
    private ExpressionStatement makeIncStatement(@NotNull final FullStatementInfo statementInfo) {
        return new ExpressionStatement(
                new MethodCallExpression(
                        newRecorderExpression(classRef, -1, -1),
                        "inc",
                        new ArgumentListExpression(
                                new ConstantExpression(statementInfo.getDataIndex()))));
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
                                                        newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(new VariableExpression(index))))
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
