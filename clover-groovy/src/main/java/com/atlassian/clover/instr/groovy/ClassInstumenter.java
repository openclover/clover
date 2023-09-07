package com.atlassian.clover.instr.groovy;

import com.atlassian.clover.api.instrumentation.InstrumentationSession;
import com.atlassian.clover.api.registry.SourceInfo;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
