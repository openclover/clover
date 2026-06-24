package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.SourceInfo;

import static org.openclover.groovy.instr.CloverAstTransformerBase.newRecorderExpression;

/**
 * Base class for more specific instrumenters.
 */
public class ClassInstumenter {
    @NotNull
    protected final InstrumentationSession session;

    @NotNull
    protected final ClassNode classRef;

    public ClassInstumenter(@NotNull final InstrumentationSession session,
                            @NotNull final ClassNode currentClass) {
        this.session = session;
        this.classRef = currentClass;
    }

    @Nullable
    public static SourceInfo countExpressionRegion(@NotNull Expression expression) {
        final ExpressionRegionTracker extentCounter = new ExpressionRegionTracker();
        expression.visit(extentCounter);
        return extentCounter.getRegion();
    }

    /**
     * Wraps {@code expr} with recorder calls that increment the true and false branch counters:
     * <pre>
     *   (expr && (iget(idx) != 0 | true)) || (iget(idx+1) == 0 &amp; false)
     * </pre>
     * When {@code expr} is truthy:  {@code iget(idx)}   is called (true branch, {@code branch.getDataIndex()}).<br>
     * When {@code expr} is falsy:   {@code iget(idx+1)} is called (false branch, {@code branch.getDataIndex() + 1}).
     * <p>
     * The {@code |} / {@code &} bitwise operators force the {@code iget()} side-effect regardless of the
     * short-circuit, while preserving the original boolean value of {@code expr}.
     */
    @NotNull
    protected Expression wrapWithBranchCounters(@NotNull final Expression expr,
                                                @NotNull final BranchInfo branch) {
        final MethodCallExpression iget_0 = new MethodCallExpression(
                newRecorderExpression(classRef, expr.getLineNumber(), expr.getColumnNumber()),
                "iget",
                new ArgumentListExpression(new ConstantExpression(branch.getDataIndex())));
        iget_0.setImplicitThis(false);
        final MethodCallExpression iget_1 = new MethodCallExpression(
                newRecorderExpression(classRef, expr.getLineNumber(), expr.getColumnNumber()),
                "iget",
                new ArgumentListExpression(new ConstantExpression(branch.getDataIndex() + 1)));
        iget_1.setImplicitThis(false);

        return new BinaryExpression(
                new BinaryExpression(
                        expr,
                        Token.newSymbol(Types.LOGICAL_AND, -1, -1),
                        new BinaryExpression(
                                new BinaryExpression(
                                        iget_0,
                                        Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1),
                                        new ConstantExpression(0)),
                                Token.newSymbol(Types.BITWISE_OR, -1, -1),
                                new ConstantExpression(Boolean.TRUE))),
                Token.newSymbol(Types.LOGICAL_OR, -1, -1),
                new BinaryExpression(
                        new BinaryExpression(
                                iget_1,
                                Token.newSymbol(Types.COMPARE_EQUAL, -1, -1),
                                new ConstantExpression(0)),
                        Token.newSymbol(Types.BITWISE_AND, -1, -1),
                        new ConstantExpression(Boolean.FALSE)));
    }
}
