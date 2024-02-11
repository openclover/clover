package com.atlassian.clover.instr.java;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$inc;

import com.atlassian.clover.registry.entities.FullStatementInfo;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.registry.FixedSourceRegion;

public class StatementInstrEmitter extends Emitter {
    private int endLine;
    private int endCol;
    private int complexity;
    private FullStatementInfo stmtInfo;

    public StatementInstrEmitter(ContextSet context, int line, int column, int endLine, int endCol) {
        this(context, line, column, endLine, endCol, 0);
    }
    public StatementInstrEmitter(ContextSet context, int line, int column, int endLine, int endCol, int complexity) {
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
            setInstr($CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(stmtInfo.getDataIndex())) + ";");
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
