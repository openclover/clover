package com.atlassian.clover.instr.java;

import com.atlassian.clover.CloverNames;

/**
 * Emits an AutoCloseable subclass declaration immediately before a try ARM block.
 * This is needed because AutoCloseable's close() method throws an exception
 * which we can't allow for compilation consistency so that means
 * we can't just create anonymous AutoCloseables in the try ARM clause.
 * We need to create anonymous ClRAutoCloseables.
 *
 * We emit a new ClRAutoCloseables declaration (they are numbered across the instrumentation session)
 * for each try ARM block because the Clover instrumenter doesn't easily support
 * forward references to types we may inject.
 */
public class AutoCloseableEmitter extends Emitter {
    public static final String AUTOCLOSEABLE_PREFIX = CloverNames.CLOVER_RECORDER_PREFIX + "$AC";

    @Override
    protected void init(InstrumentationState state) {
        if (state.isInstrEnabled()) {
            state.setDirty();
            int count = state.getAutoCloseableClassCount();
            setInstr("class " + AUTOCLOSEABLE_PREFIX + count + " implements " + state.getCfg().getJavaLangPrefix() + "AutoCloseable {public void close(){}}; ");
            state.incAutoCloseableClassCount();
        }
    }
}
