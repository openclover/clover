package com.atlassian.clover.instr.java;

import com.atlassian.clover.spi.lang.LanguageConstruct;
import com.atlassian.clover.CloverNames;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.FullStatementInfo;

import static com.atlassian.clover.instr.Bindings.$CoverageRecorder$inc;

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
