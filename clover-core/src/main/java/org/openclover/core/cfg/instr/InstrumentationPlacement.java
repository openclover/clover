package org.openclover.core.cfg.instr;

/**
 * Where Clover shall put instrumentation initializer code
 */
public enum InstrumentationPlacement {
    /**
     * Declare recorder as a class' field
     */
    FIELD,

    /**
     * Delcare recorder as an inner class
     */
    CLASS
}
