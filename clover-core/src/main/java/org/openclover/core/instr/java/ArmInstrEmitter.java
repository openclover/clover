package org.openclover.core.instr.java;

import org.openclover.core.context.ContextSet;
import org.openclover.core.context.NamedContext;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.runtime.CloverNames;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$inc;

public class ArmInstrEmitter extends Emitter {
    private int endLine;
    private int endCol;
    private int complexity;
    private FullStatementInfo stmtInfo;

    public ArmInstrEmitter(ContextSet context, int line, int column, int endLine, int endCol) {
        this(context, line, column, endLine, endCol, 0);
    }

    public ArmInstrEmitter(ContextSet context, int line, int column, int endLine, int endCol, int complexity) {
        super(context, line, column);
        this.endLine = endLine;
        this.endCol = endCol;
        this.complexity = complexity;
    }

    @Override
    public void init(InstrumentationState state) {
        stmtInfo =
            state.getSession().addStatement(
                getElementContext(),
                new FixedSourceRegion(getLine(), getColumn(), endLine, endCol),
                complexity,
                LanguageConstruct.Builtin.STATEMENT);
        if (state.isInstrEnabled()) {
            state.setDirty();

            final int autoCloseableClassIndex = state.getAutoCloseableClassCount() - 1;
            final String autoCloseableTypeName = AutoCloseableEmitter.AUTOCLOSEABLE_PREFIX + autoCloseableClassIndex;
            final int autoCloseableInstanceIndex = state.incAutoCloseableInstanceCount() - 1;
            final String autoCloseableInstanceName = CloverNames.CLOVER_PREFIX + "$ACI" + autoCloseableInstanceIndex;
            setInstr(
                autoCloseableTypeName + " " + autoCloseableInstanceName + "=new " + autoCloseableTypeName + "(){{" + $CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(stmtInfo.getDataIndex())) + ";}};"
            );
        }
    }

    @Override
    public void addContext(NamedContext ctx) {
        super.addContext(ctx);
        if (stmtInfo != null) {
            stmtInfo.addContext(ctx);
        }
    }
}
