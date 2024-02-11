package org.openclover.core.cfg.instr.java;

/**
 * Whether Clover shall instrument lambda functions (Java 8).
 */
public enum LambdaInstrumentation {
    /**
     * Do not instrument lambda functions
     */
    NONE,

    /**
     * Instrument only lambda functions declared as expressions, e.g. "(a,b) -> a + b"
     */
    EXPRESSION,

    /**
     * Instrument only lambda functions declared in code blocks, e.g. "(a,b) -> { return a + b; }"
     */
    BLOCK,

    /**
     * Instrument all (expressions and lambda blocks) lambda functions except method references e.g "(a,b) -> a + b" and
     * (a,b) -> {return a + b;} will be instrumented, but Integer::add won't
     */
    ALL_BUT_REFERENCE,

    /**
     * Instrument all lambda functions
     */
    ALL
}
