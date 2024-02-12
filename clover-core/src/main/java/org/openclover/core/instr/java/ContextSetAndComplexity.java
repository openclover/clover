package org.openclover.core.instr.java;

import org.openclover.core.api.registry.ContextSet;

/**
 * A helper class used in java.g
 */
class ContextSetAndComplexity {
    private ContextSet context;
    private int complexity;

    public static ContextSetAndComplexity empty() {
        return new ContextSetAndComplexity(null, 0);
    }

    public static ContextSetAndComplexity ofComplexity(int complexity) {
        return new ContextSetAndComplexity(null, complexity);
    }

    public void setContext(ContextSet context) {
        this.context = context;
    }

    public void addComplexity(int increment) {
        complexity += increment;
    }

    public ContextSet getContext() {
        return context;
    }

    public int getComplexity() {
        return complexity;
    }

    private ContextSetAndComplexity(ContextSet context, int complexity) {
        this.context = context;
        this.complexity = complexity;
    }

}
