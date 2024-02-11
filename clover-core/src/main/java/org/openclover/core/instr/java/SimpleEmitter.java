package org.openclover.core.instr.java;

/**
 *  a simple string emitter
 */
public class SimpleEmitter extends Emitter {
    public SimpleEmitter(String instr) {
        super();
        setInstr(instr);
    }

    @Override
    protected void init(InstrumentationState state) {
        //noop
    }
}
