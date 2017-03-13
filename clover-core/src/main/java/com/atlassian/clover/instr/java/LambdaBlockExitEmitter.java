package com.atlassian.clover.instr.java;

/**
 * Code emitter for lambda expressions declared as a code block in curly braces.
 * Emits code for the closing brace.
 */
public class LambdaBlockExitEmitter extends Emitter {
    private final LambdaBlockEntryEmitter entryEmitter;

    public LambdaBlockExitEmitter(LambdaBlockEntryEmitter entryEmitter, int endLine, int endColumn) {
        super(endLine, endColumn);
        this.entryEmitter = entryEmitter;
    }

    @Override
    protected void init(InstrumentationState state) {
        // we shall check "if (state.isInstrEnabled())" but as CLOVER:OFF could have been written in the middle
        // therefore we check for non-null value of 'method' field -> it means that enterMethod() was called
        if (entryEmitter.method != null) {
            state.getSession().exitMethod(getLine(), getColumn());
            // nothing more to do, we don't have coverage flushing in lambdas an their exit so no finally block
        }
    }
}
