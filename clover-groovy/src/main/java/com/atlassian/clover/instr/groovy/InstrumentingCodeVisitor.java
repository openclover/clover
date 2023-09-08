package com.atlassian.clover.instr.groovy;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.instrumentation.InstrumentationSession;
import com.atlassian.clover.api.registry.ContextSet;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.instr.java.JavaMethodContext;
import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.instr.tests.naming.DefaultTestNameExtractor;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import com.atlassian.clover.util.collections.Pair;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.SourceUnit;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Maps.newHashMap;

/**
 * Note: do...while is not implemented in Groovy so DoWhileStatements are ignored
 */
public class InstrumentingCodeVisitor extends ClassCodeExpressionTransformer {
    private static final Logger LOG = Logger.getInstance();
    private static final Field CLOSURE_CODE_FIELD;
    private static final ContextSet EMPTY_CONTEXT = new com.atlassian.clover.context.ContextSet();

    static {
        Field closureField;
        try {
            closureField = ClosureExpression.class.getDeclaredField("code");
            closureField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            closureField = null;
        }
        CLOSURE_CODE_FIELD = closureField;
    }

    private final InstrumentationConfig config;
    private final InstrumentationSession session;
    private final Clover2Registry registry;
    private final SourceUnit sourceUnit;
    private final TestDetector.SourceContext testSourceContext;
    private final boolean testClass;
    private final ClassNode classRef;

    private LinkedList<ClassNode> classUnderObservation = newLinkedList();
    private LinkedList<VariableScope> variableScopes = newLinkedList();
    private boolean elvisExprUsed;
    private boolean fieldExprUsed;
    private boolean safeExprUsed;
    private boolean testResultsRecorded;

    private final BranchInstrumenter branchInstrumenter;
    private final MethodInstrumenter methodInstrumenter;
    private final OperatorsInstrumenter operatorsInstrumenter;
    private final StatementInstrumenter statementInstrumenter;

    public InstrumentingCodeVisitor(
            InstrumentationConfig config,
            InstrumentationSession session,
            Clover2Registry registry,
            SourceUnit sourceUnit,
            TestDetector.SourceContext testSourceContext,
            boolean testClass,
            ClassNode classRef) {

        this.config = config;
        this.session = session;
        this.registry = registry;
        this.sourceUnit = sourceUnit;
        this.testSourceContext = testSourceContext;
        this.testClass = testClass;
        this.classRef = classRef;
        this.branchInstrumenter = new BranchInstrumenter(session, classRef);
        this.statementInstrumenter = new StatementInstrumenter(session, classRef, config.isStatementInstrEnabled());
        this.methodInstrumenter = new MethodInstrumenter(classRef, config.isRecordTestResults(),
                config.isIntervalBasedFlushing(), config.getFlushPolicy());
        this.operatorsInstrumenter = new OperatorsInstrumenter(session, classRef, branchInstrumenter);
    }

    public boolean isElvisExprUsed() {
        return elvisExprUsed;
    }

    public boolean isFieldExprUsed() {
        return fieldExprUsed;
    }

    public boolean isSafeExprUsed() {
        return safeExprUsed;
    }

    public boolean isTestResultsRecorded() {
        return testResultsRecorded;
    }

    public Map<String, MethodNode> getSafeEvalMethods() {
        return operatorsInstrumenter.getSafeEvalMethods();
    }

    private void pushVariableScope(VariableScope topPush) {
        if (topPush != null) {
            variableScopes.add(topPush);
        }
    }

    private void popVariableScope(VariableScope pushed) {
        if (pushed != null) {
            variableScopes.removeLast();
        }
    }

    public VariableScope getCurrentVariableScope() {
        return variableScopes.isEmpty() ? null : variableScopes.getLast();
    }

    public ClassNode getCurrentClassNode() {
        return classUnderObservation.isEmpty() ? null : classUnderObservation.getLast();
    }

    private void popClass() {
        classUnderObservation.removeLast();
    }

    private void pushClass(ClassNode node) {
        classUnderObservation.add(node);
    }

    private ContextSet currentMethodContext() {
        FullMethodInfo currentMethod = (FullMethodInfo) session.getCurrentMethod(); // use the current method's context
        return currentMethod != null ? currentMethod.getContext() : EMPTY_CONTEXT;
    }

    ///CLOVER:OFF
    @Override
    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }
    ///CLOVER:ON

    private String scriptClassNameForFileName(String fileName) {
        return "script@" + fileName.replaceAll("\\s", "_");
    }

    public boolean instrument(ClassNode clazz) {
        visitClass(clazz);
        return isInstrumentable(clazz);
    }

    @Override
    public void visitClass(ClassNode clazz) {
        pushClass(clazz);
        session.enterClass(
                GroovyUtils.isScriptClass(clazz) ?
                        scriptClassNameForFileName(Grover.getSourceUnitFile(sourceUnit).getName())
                        : clazz.getNameWithoutPackage(),
                GroovyUtils.newRegionFor(clazz, true),
                GroovyModelMiner.extractModifiers(clazz),
                clazz.isInterface(), clazz.isDerivedFrom(ClassHelper.Enum_Type), clazz.isAnnotationDefinition());

        if (isInstrumentable(clazz)) {
            super.visitClass(clazz);
        }

        session.exitClass(clazz.getLastLineNumber(), clazz.getLastColumnNumber());
        popClass();
    }

    @Override
    protected void visitConstructorOrMethod(MethodNode method, boolean isConstructor) {
        LOG.debug("Called visitConstructorOrMethod(" + method.getName() + ", " + isConstructor + ")");

        if (isInstrumentable(method)) {
            boolean isScriptRun = GroovyUtils.isScriptClass(method.getDeclaringClass()) && "run".equals(method.getName());

            final FixedSourceRegion srcRegion =
                isScriptRun
                    ? GroovyUtils.newRegionFor(method.getDeclaringClass())
                    : GroovyUtils.newRegionFor(method);

            final Statement methodEntry;
            //A null srcRegion indicates a synthetic method possibly for implementing default arguments
            if (srcRegion != null) {
                pushVariableScope(method.getVariableScope());
                final Map<String, ClassNode> annotationClassNodes = newHashMap();
                final MethodSignature signature = GroovyModelMiner.extractMethodSignature(method, annotationClassNodes);
                final boolean isLambda = false;
                final boolean isTestMethod =
                    testClass
                        && config.getTestDetector().isMethodMatch(testSourceContext, JavaMethodContext.createFor(signature));

                // register method in the model ...
                final FullMethodInfo methodInfo = (FullMethodInfo) session.enterMethod(EMPTY_CONTEXT, srcRegion, signature,
                        isTestMethod, null, isLambda, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, LanguageConstruct.Builtin.METHOD);
                // and update it adding information about static name of a test
                methodInfo.setStaticTestName(DefaultTestNameExtractor.INSTANCE.getTestNameForMethod(methodInfo));

                methodEntry = Grover.recorderInc(classRef, methodInfo, method.getCode());
                matchContext(signature, methodInfo);

                if (config.isStatementInstrEnabled()) {
                    visitClassCodeContainer(method.getCode());
                    if (method.hasDefaultValue()) {
                        gatherStmtsFromSimilarSynthMethods(method);
                        visitParamInitialValues(method);
                    }
                    // special handling for Grails' @Transactional annotation
                    gatherStatementsFromTransactionalSyntheticMethods(method);
                }
                final boolean methodEntryInTryCatch = isTestMethod || config.isIntervalBasedFlushing();
                final Pair<Statement, Boolean> wrappedMethod = methodInstrumenter.wrapMethodInTryCatch(
                        getCurrentClassNode(),
                        method,
                        methodInfo,
                        methodInfo.getDataIndex(),
                        methodEntry,
                        statementInstrumenter.instrumentBlockStatement(
                                method.getCode(),
                                methodEntryInTryCatch ? null : methodEntry,
                                getCurrentVariableScope()),
                        annotationClassNodes);
                method.setCode(wrappedMethod.first);
                if (wrappedMethod.second) {
                    this.testResultsRecorded = true;
                }
                session.exitMethod(method.getLastLineNumber(), method.getLastColumnNumber());
                popVariableScope(method.getVariableScope());
            }
        }
    }

    public boolean isInstrumentable(ClassNode clazz) {
        // TODO: CLOV-1960 instrument traits
        return !clazz.isAnnotationDefinition() && !clazz.isInterface();
    }

    private boolean isInstrumentable(MethodNode method) {
        return !method.isAbstract() && !method.isSynthetic();
    }

    private void matchContext(MethodSignature signature, FullMethodInfo methodInfo) {
        final List<MethodRegexpContext> methodContexts = registry.getContextStore().getMethodContexts();
        final String normalizedSignature = signature.getNormalizedSignature();
        if (Logger.isVerbose()) {
            LOG.verbose("Matching normalized method signature'" + normalizedSignature + "'. Signature info: " + signature);
        }
        for (MethodRegexpContext methodContext : methodContexts) {
            // TODO consider methodContext.getMaxComplexity too? For Groovy, this may only be available at report time? NB: this feature currently not documented for java...
            final boolean matches = methodContext.matches(normalizedSignature);
            if (matches) {
                methodInfo.addContext(methodContext);
                if (Logger.isVerbose()) {
                    LOG.verbose("Method context: '" + methodContext.getPattern() + "' matched method: " + normalizedSignature);
                }
            }
        }
    }

    @Override
    public Expression transform(Expression expr) {
        Expression transformed = super.transform(expr);
        if (transformed instanceof ElvisOperatorExpression) {
            final Pair<ElvisOperatorExpression, Boolean> ret = operatorsInstrumenter.transformElvis(
                    (ElvisOperatorExpression) transformed, currentMethodContext());
            transformed = ret.first;
            elvisExprUsed |= ret.second;
        } else if (transformed instanceof TernaryExpression) {
            transformed = operatorsInstrumenter.transformTernary(
                    (TernaryExpression) transformed, currentMethodContext());
        } else if (transformed instanceof AttributeExpression) {
            final AttributeExpression attributeExpression = (AttributeExpression) transformed;
            if (attributeExpression.isSafe()) {
                final Pair<AttributeExpression, Boolean> ret = operatorsInstrumenter.transformSafeAttributeExpression(
                        attributeExpression, getCurrentClassNode(), currentMethodContext());
                transformed = ret.first;
                safeExprUsed |= ret.second;
            }
        } else if (transformed instanceof PropertyExpression) {
            final PropertyExpression propertyExpression = (PropertyExpression)transformed;
            if (propertyExpression.isSafe()) {
                final Pair<PropertyExpression, Boolean> ret = operatorsInstrumenter.transformSafeProperty(
                        propertyExpression, getCurrentClassNode(), currentMethodContext());
                transformed = ret.first;
                safeExprUsed |= ret.second;
            }
        } else if (transformed instanceof MethodCallExpression) {
            final MethodCallExpression methodCallExpression = (MethodCallExpression) transformed;
            if (methodCallExpression.isSafe()) {
                final Pair<MethodCallExpression, Boolean> ret = operatorsInstrumenter.transformSafeMethodCall(
                        methodCallExpression, getCurrentClassNode(), currentMethodContext());
                transformed = ret.first;
                safeExprUsed |= ret.second;
            }
        } else if (transformed instanceof ConstructorCallExpression
                && isAnonymousInnerClassCall((ConstructorCallExpression) transformed)) {
            final ConstructorCallExpression ctorCall = (ConstructorCallExpression) transformed;
            for (MethodNode method : ctorCall.getType().getMethods()) {
                if (isInstrumentable(method)) {
                    method.setCode(statementInstrumenter.instrumentBlockStatement(method.getCode()));
                }
            }
            for (FieldNode field : ctorCall.getType().getFields()) {
                if (field.hasInitialExpression()) {
                    //Visit sub-expressions incase there are closures there
                    field.getInitialValueExpression().visit(this);
                    field.setInitialValueExpression(transform(field.getInitialValueExpression()));
                }
            }
        }
        transformed.setSourcePosition(expr);
        return transformed;
    }

    private boolean isAnonymousInnerClassCall(ConstructorCallExpression call) {
        try {
            //1.7+ method
            return (Boolean) call.getClass().getMethod("isUsingAnonymousInnerClass", new Class[]{}).invoke(call);
        } catch (Exception e) {
            return false;
        }
    }


    private void visitParamInitialValues(MethodNode method) {
        Parameter[] paras = method.getParameters();
        for (Parameter p : paras) {
            if (p.hasInitialExpression()) {
                p.getInitialExpression().visit(this);
                p.setInitialExpression(transform(p.getInitialExpression()));
            }
        }
    }

    /**
     * For older versions of Groovy if the method has parameters with defaults there will be one or more
     * synthetic methods implementing these defaults but with -1 line numbers
     * If there are any params with closures as defaults we want to instrument these but in the
     * context of the original method
     */
    @SuppressWarnings("unchecked")
    private void gatherStmtsFromSimilarSynthMethods(MethodNode method) {
        final List<MethodNode> otherLikeMethods = getCurrentClassNode().getMethods(method.getName());

        for (MethodNode otherLikeMethod : otherLikeMethods) {
            if (!GroovyUtils.isReportable(otherLikeMethod) && sharesZeroOrMoreParameters(method.getParameters(), otherLikeMethod.getParameters())) {
                LOG.debug("Instrumenting code of a synthetic method " + otherLikeMethod.getName());
                visitClassCodeContainer(otherLikeMethod.getCode());
            }
        }
    }

    /**
     * Look for Grails' @Transactional synthetic methods. They:
     *  - are named like "$tt__methodName"
     *  - have (-1, -1, -1, -1) source region for the MethodNode
     *  - have an extra TransactionStatus argument in their signature (last position)
     *  - contain code of the original method
     *
     * @param method a method for which we look for synthehic methods
     */
    private void gatherStatementsFromTransactionalSyntheticMethods(MethodNode method) {
        String syntheticName = "$tt__" + method.getName();
        final List<MethodNode> otherLikeMethods = getCurrentClassNode().getMethods(syntheticName);
        for (MethodNode otherLikeMethod : otherLikeMethods) {
            if (!GroovyUtils.isReportable(otherLikeMethod) &&
                    sharesTransactionAndParameters(method.getParameters(), otherLikeMethod.getParameters())) {
                LOG.debug("Instrumenting code of a transactional method " + otherLikeMethod.getName());
                otherLikeMethod.setCode(
                        statementInstrumenter.instrumentBlockStatement(otherLikeMethod.getCode()));
            }
        }
    }

    /**
     * Syntetic methods for methods having default values for their arguments shall contain a subset
     * of original arguments.
     * @param parameters original method
     * @param likeParameters potential synthetic method
     * @return true if they "match"
     */
    private boolean sharesZeroOrMoreParameters(Parameter[] parameters, Parameter[] likeParameters) {
        final int minParams = Math.min(parameters.length, likeParameters.length);

        for (int i = 0; i < minParams; i++) {
            final Parameter parameter = parameters[i];
            final Parameter likeParameter = likeParameters[i];
            if (!parameter.getName().equals(likeParameter.getName())
                || !parameter.getType().equals(likeParameter.getType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Synthetic methods produced by @Transactional annotations have the org.springframework.transaction.TransactionStatus
     * as the first argument, followed by arguments of the original method
     * @param originalParameters list of parameters of the original method
     * @param likeParameters list of parameters of the similar method, possibly synthetic
     * @return true if they "match"
     */
    private boolean sharesTransactionAndParameters(Parameter[] originalParameters, Parameter[] likeParameters) {
        // quick check if we have (..., TransationStatus)
        if (likeParameters.length <= 0 ||
                originalParameters.length + 1 != likeParameters.length ||
                !likeParameters[likeParameters.length - 1].getType().getName().equals(
                        "org.springframework.transaction.TransactionStatus")) {
            return false;
        }

        // compare arguments one by one
        return sharesZeroOrMoreParameters(originalParameters, likeParameters);
    }

    @Override
    public void visitSwitch(SwitchStatement statement) {
        statement.getExpression().visit(this);
        statement.setExpression(transform(statement.getExpression()));

        for (CaseStatement caseStatement : statement.getCaseStatements()) {
            caseStatement.visit(this);
        }

        statement.getDefaultStatement().visit(this);
        statement.setDefaultStatement(statementInstrumenter.instrumentBlockStatement(statement.getDefaultStatement()));
    }

    @Override
    public void visitField(FieldNode field) {
        if (field.hasInitialExpression()) {
            final FixedSourceRegion srcRegion = GroovyUtils.newRegionFor(field);

            if (srcRegion != null) {
                // TODO: also filter fields using method contexts ?
                final FullMethodInfo methodInfo = (FullMethodInfo) session.enterMethod(
                        EMPTY_CONTEXT, srcRegion,
                        new MethodSignature("field " + field.getName(), field.getModifiers(), GroovyModelMiner.extractAnnotations(field)),
                        false, null, false, 0, LanguageConstruct.Builtin.GROOVY_FIELD_EXPRESSION);
                // note: fields do not have static test name

                //Visit sub-expressions incase there are closures there
                field.getInitialValueExpression().visit(this);

                final StaticMethodCallExpression exprEvalCall =
                    new StaticMethodCallExpression(
                        classRef,
                        CloverNames.namespace("exprEval"),
                        new ArgumentListExpression(
                            transform(field.getInitialValueExpression()),
                            new ConstantExpression(methodInfo.getDataIndex())));


                field.setInitialValueExpression(
                    field.isDynamicTyped()
                        ? exprEvalCall
                        : new CastExpression(field.getType(), exprEvalCall));

                session.exitMethod(srcRegion.getEndLine(), srcRegion.getEndColumn());

                fieldExprUsed = true;
            }
        }
    }

    @Override
    public void visitProperty(PropertyNode node) {
        visitClassCodeContainer(node.getGetterBlock());
        node.setGetterBlock(statementInstrumenter.instrumentBlockStatement(node.getGetterBlock()));

        visitClassCodeContainer(node.getSetterBlock());
        node.setSetterBlock(statementInstrumenter.instrumentBlockStatement(node.getSetterBlock()));
    }

    @Override
    public void visitIfElse(final IfStatement ifElse) {
        ifElse.getBooleanExpression().visit(this);
        ifElse.setBooleanExpression(
                branchInstrumenter.transformBranch(
                        ClassInstumenter.countExpressionRegion(ifElse.getBooleanExpression()),
                        ifElse.getBooleanExpression(),
                        currentMethodContext()));

        ifElse.getIfBlock().visit(this);
        ifElse.setIfBlock(statementInstrumenter.instrumentBlockStatementOrExpressionStatement(
                getCurrentVariableScope(), ifElse.getIfBlock()));

        ifElse.getElseBlock().visit(this);
        ifElse.setElseBlock(statementInstrumenter.instrumentBlockStatementOrExpressionStatement(
                getCurrentVariableScope(), ifElse.getElseBlock()));
    }

    @Override
    public void visitAnnotations(AnnotatedNode node) {
        //We are not interested in annotations as their values are compile-time constants
    }

    @Override
    public void visitReturnStatement(ReturnStatement statement) {
        statement.getExpression().visit(this);
        statement.setExpression(transform(statement.getExpression()));
    }

    @Override
    public void visitAssertStatement(AssertStatement as) {
        as.getBooleanExpression().visit(this);
        as.setBooleanExpression((BooleanExpression) (transform(as.getBooleanExpression())));
        as.setMessageExpression(transform(as.getMessageExpression()));
    }

    @Override
    public void visitCaseStatement(CaseStatement statement) {
        statement.getExpression().visit(this);
        statement.setExpression(transform(statement.getExpression()));
        statement.getCode().visit(this);
        statement.setCode(statementInstrumenter.instrumentBlockStatementOrExpressionStatement(
                getCurrentVariableScope(), statement.getCode()));
    }

    @Override
    public void visitForLoop(ForStatement forLoop) {
        pushVariableScope(forLoop.getVariableScope());
        if (forLoop.getCollectionExpression() instanceof ClosureListExpression
            && ((ClosureListExpression)forLoop.getCollectionExpression()).getExpressions().size() >= 3) {
            final ClosureListExpression closureList = (ClosureListExpression) forLoop.getCollectionExpression();

            //Middle expression is the conditional expression
            final int condIndex = (closureList.getExpressions().size() - 1) / 2;
            final Expression condExpr = closureList.getExpressions().get(condIndex);

            if (condExpr != EmptyExpression.INSTANCE) {
                final SourceInfo condRegion = ClassInstumenter.countExpressionRegion(condExpr);

                closureList.getExpressions().set(
                        condIndex,
                        branchInstrumenter.transformBranch(
                                condRegion,
                                condExpr instanceof BooleanExpression
                                        ? (BooleanExpression) condExpr
                                        : new BooleanExpression(condExpr),
                                currentMethodContext()
                        )
                );
            }
        }
        forLoop.getCollectionExpression().visit(this);
        forLoop.setCollectionExpression(transform(forLoop.getCollectionExpression()));
        forLoop.getLoopBlock().visit(this);
        forLoop.setLoopBlock(
                statementInstrumenter.instrumentBlockStatementOrExpressionStatement(
                        getCurrentVariableScope(), forLoop.getLoopBlock())
        );
        popVariableScope(forLoop.getVariableScope());
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatement sync) {
        sync.getExpression().visit(this);
        sync.setExpression(transform(sync.getExpression()));
        sync.getCode().visit(this);
        sync.setCode(statementInstrumenter.instrumentBlockStatement(sync.getCode()));
    }

    @Override
    public void visitThrowStatement(final ThrowStatement ts) {
        ts.getExpression().visit(this);
        ts.setExpression(transform(ts.getExpression()));
    }

    @Override
    public void visitWhileLoop(final WhileStatement loop) {
        loop.getBooleanExpression().visit(this);
        loop.setBooleanExpression(branchInstrumenter.transformBranch(
                ClassInstumenter.countExpressionRegion(loop.getBooleanExpression()),
                loop.getBooleanExpression(),
                currentMethodContext())
        );
        loop.getLoopBlock().visit(this);
        loop.setLoopBlock(statementInstrumenter.instrumentBlockStatementOrExpressionStatement(getCurrentVariableScope(), loop.getLoopBlock()));
    }

    @Override
    public void visitExpressionStatement(ExpressionStatement es) {
        es.getExpression().visit(this);
        es.setExpression(transform(es.getExpression()));
    }

    @Override
    public void visitCatchStatement(CatchStatement statement) {
        statement.getCode().visit(this);
        statement.setCode(statementInstrumenter.instrumentBlockStatement(statement.getCode()));
    }

    @Override
    public void visitTryCatchFinally(TryCatchStatement statement) {
        statement.getTryStatement().visit(this);
        statement.setTryStatement(statementInstrumenter.instrumentBlockStatement(statement.getTryStatement()));

        for (CatchStatement catchStatement : statement.getCatchStatements()) {
            catchStatement.visit(this);
        }

        statement.getFinallyStatement().visit(this);
        // finally blocks are doubly-nested. The outer-most referes to the start of "finally {..." and the second
        // refers to the start of "{..."; the inner block will be instrumented by visit(this) -> visitBlockStatement
        // so we don't call statementInstrumenter here
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression) {
        pushVariableScope(expression.getVariableScope());
        expression.getCode().visit(this);
        setClosureCode(expression, statementInstrumenter.instrumentBlockStatement(expression.getCode()));
        popVariableScope(expression.getVariableScope());
    }

    @Override
    public void visitClosureListExpression(ClosureListExpression expression) {
        pushVariableScope(expression.getVariableScope());
        super.visitClosureListExpression(expression);
        popVariableScope(expression.getVariableScope());
    }

    @Override
    public void visitBlockStatement(BlockStatement statement) {
        pushVariableScope(statement.getVariableScope());
        super.visitBlockStatement(statement);

        // blocks may contain other blocks inside, we have to instrument them too; we don't instrument all statements
        // in the current block because the current block was already instrumented in other callbacks
        // (ifElse.setIfBlock, method.setCode ...) and has original statments interleaved with R.inc() calls
        // we must avoid double instrumentation
        final List<Statement> statements = statement.getStatements();
        for (int i = 0; i < statements.size(); i++) {
            Statement s = statements.get(i);
            if (s instanceof BlockStatement) {
                statements.set(i, statementInstrumenter.instrumentBlockStatement(s));
            }
        }

        popVariableScope(statement.getVariableScope());
    }

    private void setClosureCode(ClosureExpression expression, Statement statement) {
        if (CLOSURE_CODE_FIELD != null) {
            try {
                CLOSURE_CODE_FIELD.set(expression, statement);
            } catch (Exception e) {
                LOG.debug("Failed to set closure code, expression=" + expression + " statement=" + statement);
            }
        } else {
            LOG.debug("Failed to set closure code as CLOSURE_CODE_FIELD is null");
        }
    }

}
