package org.openclover.groovy.instr;

import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Instrumenting code branches. Instantiate this class per every Groovy class instrumented.
 */
public class BranchInstrumenter extends ClassInstumenter {

    public BranchInstrumenter(@NotNull final InstrumentationSession session, @NotNull final ClassNode currentClass) {
        super(session, currentClass);
    }

    @NotNull
    public BooleanExpression transformBranch(@Nullable final SourceInfo srcRegion,
                                             @NotNull final BooleanExpression exp,
                                             @NotNull final ContextSet currentMethodContext) {
        if (srcRegion != null) {
            final BranchInfo branch = session.addBranch(currentMethodContext, srcRegion, true,
                    1 + ExpressionComplexityCounter.count(exp), LanguageConstruct.Builtin.BRANCH);

            final MethodCallExpression iget_0 = new MethodCallExpression(
                    Grover.newRecorderExpression(classRef, exp.getLineNumber(), exp.getColumnNumber()),
                    "iget",
                    new ArgumentListExpression(new ConstantExpression(branch.getDataIndex())));
            iget_0.setImplicitThis(false); // we don't need 'this' in our method call context
            final MethodCallExpression iget_1 = new MethodCallExpression(
                    Grover.newRecorderExpression(classRef, exp.getLineNumber(), exp.getColumnNumber()),
                    "iget",
                    new ArgumentListExpression(new ConstantExpression(branch.getDataIndex() + 1)));
            iget_1.setImplicitThis(false); // we don't need 'this' in our method call context

            return new BooleanExpression(
                    new BinaryExpression(
                            new BinaryExpression(
                                    exp,
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
                                    new ConstantExpression(Boolean.FALSE)
                            )));
        } else {
            return exp;
        }
    }
}
