package org.openclover.groovy.instr;

import org.openclover.runtime.CloverNames;
import com.atlassian.clover.instr.tests.ExpectedExceptionMiner;
import org.openclover.runtime.recorder.PerTestRecorder;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.util.collections.Pair;
import org_openclover_runtime.CoverageRecorder;
import org_openclover_runtime.TestNameSniffer;
import groovyjarjarasm.asm.Opcodes;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Lists.newLinkedList;

/**
 * Wrapping test methods into a try-catch block.
 */
public class MethodInstrumenter {
    private final ClassNode classRef;
    private final boolean isRecordingTestResults;
    private final boolean isIntervalFlushing;
    private final int flushPolicy;

    public MethodInstrumenter(@NotNull final ClassNode classRef,
                              final boolean isRecordingTestResults,
                              final boolean isIntervalFlushing,
                              final int flushPolicy) {
        this.classRef = classRef;
        this.isRecordingTestResults = isRecordingTestResults;
        this.isIntervalFlushing = isIntervalFlushing;
        this.flushPolicy = flushPolicy;
    }

    /**
     * FOR TEST REWRITING:
     * <pre>
     * \@Test
     * \@Expected([RuntimeException.class, FooException.class])
     * def testFoo() {
     *   someStatements()
     * }
     * </pre>
     * <p/>
     * <pre>
     * \@Test
     * \@Expected([RuntimeException.class, FooException.class])
     * def testFoo() {
     *   CLASS.R().globalSliceStart(getClass().getName(), $idx$)
     *   CLASS.R().inc($idx$)
     *   Throwable exception = null
     *   try {
     *     someStatements()
     *   } catch (Throwable t) {
     *     exception = t
     *     throw t
     *   } finally {
     *     CLASS.R().globalSliceEnd(
     *       getClass().getName(),
     *       "testFoo",
     *       CLASS.__CLRx_y_z_TEST_NAME_SNIFFER.getTestName(),
     *       $idx$,
     *       RECORDERCLASS.evalTestException(exception, []),
     *       exception)
     *   }
     * }
     * </pre>
     * FOR FLUSH REWRITING:
     * <pre>
     * def testFoo() {
     *   someStatements()
     * }
     * </pre>
     * <p/>
     * <pre>
     * def testFoo() {
     *   try {
     *     someStatements()
     *   } finally {
     *     CLASS.R().maybeFlush() OR CLASS.R().flushNeeded()
     *   }
     * }
     * </pre>
     * @return Pair&lt;Statement, Boolean&gt; - new statement, true if testResults are recorded
     */
    @NotNull
    public Pair<Statement, Boolean> wrapMethodInTryCatch(
            @NotNull final ClassNode currentClassNode,
            @NotNull final MethodNode method,
            @NotNull final FullMethodInfo methodInfo,
            int index,
            @NotNull Statement methodEntryInc, @NotNull Statement maybeBlockStatement,
            @NotNull final Map<String, ClassNode> annotationClassNodes) {
        final boolean isTest = methodInfo.isTest();

        if (isTest || isIntervalFlushing) {
            if (maybeBlockStatement instanceof BlockStatement) {

                final BlockStatement methodStatements = (BlockStatement) maybeBlockStatement;
                final BlockStatement methodReplacement = new BlockStatement(new LinkedList<>(), method.getVariableScope());

                if (isTest) {
                    methodReplacement.addStatement(
                            new ExpressionStatement(
                                    new MethodCallExpression(
                                            Grover.newRecorderExpression(classRef, method.getLineNumber(), method.getColumnNumber()),
                                            "globalSliceStart",
                                            new ArgumentListExpression(
                                                    new Expression[]{
                                                            classNameExpression(method, currentClassNode),
                                                            new ConstantExpression(index)
                                                    }))));
                }

                //Method entry should be flagged after slice start so that the test method is considered to have been touched by the test
                methodReplacement.addStatement(methodEntryInc);

                final VariableExpression exceptionVar = new VariableExpression(CloverNames.namespace("exception"), ClassHelper.make(Throwable.class));
                if (isTest && isRecordingTestResults) {
                    methodReplacement.addStatement(
                            new ExpressionStatement(
                                    new DeclarationExpression(
                                            exceptionVar,
                                            Token.newSymbol(Types.ASSIGN, -1, -1),
                                            ConstantExpression.NULL)));
                }

                final BlockStatement tryBlock = new BlockStatement();
                tryBlock.setVariableScope(methodReplacement.getVariableScope());
                //Add all the original statements in the try { ... }
                tryBlock.addStatements(methodStatements.getStatements());

                BlockStatement finallyBlock = new BlockStatement();
                finallyBlock.setVariableScope(new VariableScope(methodReplacement.getVariableScope()));

                if (isIntervalFlushing) {
                    finallyBlock.addStatement(
                            new ExpressionStatement(
                                    new MethodCallExpression(
                                            Grover.newRecorderExpression(classRef, -1, -1),
                                            flushPolicy == CoverageRecorder.FLUSHPOLICY_INTERVAL
                                                    ? "maybeFlush"
                                                    : "flushNeeded",
                                            ArgumentListExpression.EMPTY_ARGUMENTS)));
                }

                if (isTest) {
                    // call "CLASS.__CLRx_y_z_TEST_NAME_SNIFFER.getTestName()"
                    // see TestNameSniffer#getTestName()
                    Expression callSnifferGetTestName = new MethodCallExpression(
                            new FieldExpression(
                                    new FieldNode(
                                            CloverNames.CLOVER_TEST_NAME_SNIFFER,
                                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                            ClassHelper.make(TestNameSniffer.class),
                                            classRef, null)),
                            "getTestName", ArgumentListExpression.EMPTY_ARGUMENTS);

                    //CLASS.R().globalSliceEnd(
                    //  getClass().getName(),
                    //  "testFoo",
                    //  CLASS.__CLRx_y_z_TEST_NAME_SNIFFER.getTestName(),
                    //  $idx$,
                    //  CLASS.R().evalTestException(exception, [RuntimeException.class, FooException.class]),
                    //  exception)
                    finallyBlock.addStatement(
                            new ExpressionStatement(
                                    new MethodCallExpression(
                                            Grover.newRecorderExpression(classRef, -1, -1),
                                            "globalSliceEnd",
                                            new ArgumentListExpression(
                                                    new Expression[]{
                                                            classNameExpression(method, currentClassNode),
                                                            new ConstantExpression(methodInfo.getQualifiedName()),
                                                            callSnifferGetTestName,
                                                            new ConstantExpression(index),
                                                            isRecordingTestResults
                                                                    ? new StaticMethodCallExpression(
                                                                    classRef,
                                                                    CloverNames.namespace("evalTestException"),
                                                                    new ArgumentListExpression(
                                                                            newArrayList(
                                                                                    exceptionVar,
                                                                                    new ListExpression(expectedExceptions(methodInfo, annotationClassNodes)))))
                                                                    : new ConstantExpression(PerTestRecorder.NO_EXIT_RESULT),
                                                            isRecordingTestResults
                                                                    ?  exceptionVar
                                                                    : ConstantExpression.NULL
                                                    }))));
                }

                final TryCatchStatement tryCatch = new TryCatchStatement(tryBlock, finallyBlock);
                methodReplacement.addStatement(tryCatch);

                if (isTest && isRecordingTestResults) {
                    final BlockStatement catchBlock = new BlockStatement();
                    catchBlock.setVariableScope(new VariableScope(methodReplacement.getVariableScope()));
                    final Parameter tVariable = new Parameter(ClassHelper.make(Throwable.class), CloverNames.namespace("t"));

                    //exception = t
                    catchBlock.addStatement(
                            new ExpressionStatement(
                                    new BinaryExpression(
                                            exceptionVar,
                                            Token.newSymbol(Types.ASSIGN, -1, -1),
                                            new VariableExpression(tVariable)
                                    )));
                    //throw t
                    catchBlock.addStatement(new ThrowStatement(new VariableExpression(tVariable)));
                    tryCatch.addCatch(new CatchStatement(tVariable, catchBlock));
                }

                return Pair.<Statement, Boolean>of(methodReplacement, isRecordingTestResults);
            }
        }

        return Pair.of(maybeBlockStatement, false);
    }

    @NotNull
    private Expression classNameExpression(
            @NotNull final MethodNode method,
            @NotNull final ClassNode currentClassNode) {
        if (method.isStatic()) {
            return new MethodCallExpression(
                    new ClassExpression(currentClassNode),
                    new ConstantExpression("getName"),
                    new ArgumentListExpression());
        } else {
            return new MethodCallExpression(
                    new MethodCallExpression(new VariableExpression("this"), new ConstantExpression("getClass"), new ArgumentListExpression()),
                    new ConstantExpression("getName"),
                    new ArgumentListExpression());
        }
    }

    @NotNull
    private List<Expression> expectedExceptions(
            @NotNull final FullMethodInfo methodInfo,
            @NotNull final Map<String, ClassNode> annotationClassNodes) {
        final String[] expectedExceptionNames = ExpectedExceptionMiner.extractExpectedExceptionsFor(methodInfo.getSignature(), false);
        final List<Expression> expectedExceptions = newLinkedList();
        for (String expectedExceptionName : expectedExceptionNames) {
            final ClassNode expectedExceptionClass = annotationClassNodes.get(expectedExceptionName);
            if (expectedExceptionClass != null) {
                expectedExceptions.add(new ClassExpression(expectedExceptionClass));
            }
        }
        return expectedExceptions;
    }

}
