package com.atlassian.clover.instr.java;

/**
 * Code emitter for lambda expressions declared as an expression to be converted into curly braces. Emits code for the
 * closing brace.
 */
public class LambdaExprToBlockExitEmitter extends Emitter {

    protected final LambdaExprToBlockStartEntryEmitter entryEmitter;

    public LambdaExprToBlockExitEmitter(LambdaExprToBlockStartEntryEmitter entryEmitter, int endLine, int endColumn) {
        super(endLine, endColumn);
        this.entryEmitter = entryEmitter;
    }

    @Override
    protected void init(InstrumentationState state) {
        if (entryEmitter.method != null) {
            state.getSession().exitMethod(getLine(), getColumn());
            setInstr(";}");
        }
    }
}
