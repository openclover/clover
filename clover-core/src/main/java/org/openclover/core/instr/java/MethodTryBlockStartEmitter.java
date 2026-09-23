package org.openclover.core.instr.java;

/**
 * Opens the try block ('try{') of a method for which {@link MethodEntryInstrEmitter} deferred it, i.e. a
 * constructor in Java 25+ (JEP 513). Its matching '}finally{...}' is emitted by {@link MethodExitInstrEmitter}.
 * It must be placed right after the explicit super()/this() invocation, because such an invocation is not
 * allowed to be inside a try block.
 */
public class MethodTryBlockStartEmitter extends Emitter {

    private final MethodEntryInstrEmitter entry;

    public MethodTryBlockStartEmitter(MethodEntryInstrEmitter entryEmitter) {
        this.entry = entryEmitter;
    }

    @Override
    public void init(InstrumentationState state) {
        // the entry emitter is initialised earlier (it precedes the invocation in the token stream)
        setInstr(state.isInstrEnabled() && entry.needsFinally() ? "try{" : "");
    }
}
