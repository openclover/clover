package com.atlassian.clover.instr.java;

public class StartBoolInstrEmitter extends Emitter {
    private final ExpressionInfo expr;

    public StartBoolInstrEmitter(ExpressionInfo expr) {
        super();
        this.expr = expr;
    }

    @Override
    protected void init(InstrumentationState state) {
        //Matches the emission condition in EndBoolInstrEmitter
        if (state.isInstrEnabled() && state.getSession().getCurrentMethod() != null && expr.isInstrumentable()) {
            state.setDirty();
            setInstr("(((");
        }
    }
}
