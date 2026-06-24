package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.spi.lang.LanguageConstruct;

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
            return new BooleanExpression(wrapWithBranchCounters(exp, branch));
        } else {
            return exp;
        }
    }
}
