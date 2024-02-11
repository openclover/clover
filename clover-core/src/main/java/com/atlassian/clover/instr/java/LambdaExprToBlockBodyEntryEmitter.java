package com.atlassian.clover.instr.java;

import org.openclover.runtime.instr.Bindings;

/**
 * Code emitter for lambda expressions declared as an expression to be translated into curly braces. Emits code for the
 * opening brace and return statement if currentMethod#isVoidReturnType==false. Registered at lambda body start.
 */
public class LambdaExprToBlockBodyEntryEmitter extends Emitter {

    private final LambdaExprToBlockStartEntryEmitter startEmitter;

    public LambdaExprToBlockBodyEntryEmitter(LambdaExprToBlockStartEntryEmitter startEmitter, int startLine, int startColumn) {
        super(startLine, startColumn);
        this.startEmitter = startEmitter;
    }

    @Override
    protected void init(InstrumentationState state) {
        if (shouldInstrument()) {
            StringBuilder instr = new StringBuilder("{");
            instr.append(Bindings.$CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(startEmitter.method.getDataIndex())));
            instr.append(";");
            if (!startEmitter.method.isVoidReturnType()) {
                instr.append("return ");
            }
            setInstr(instr.toString());
        }
    }

    private boolean shouldInstrument() {
        return startEmitter.method != null && startEmitter.method.isLambda();
    }
}
