package com.atlassian.clover.instr.java;


import static org.openclover.runtime.instr.Bindings.$Clover$globalFlush;

public class DirectedFlushEmitter extends Emitter {
    @Override
    public void init(InstrumentationState state) {
        if (state.needsFlush() && state.isInstrEnabled()) {
            setInstr($Clover$globalFlush() + ";");
            state.setNeedsFlush(false);
        }
    }
}
