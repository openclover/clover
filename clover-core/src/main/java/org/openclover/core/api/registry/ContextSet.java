package org.openclover.core.api.registry;

/**
 * Set of code contexts - for built-in language constructs like if-else block, for loop etc as well as
 * for custom statement / method contexts.
 */
public interface ContextSet {
    ContextSet set(int index);

    ContextSet set(int bitIndex, boolean value);

    ContextSet flip(int startIdx, int endIdx);

    ContextSet clear(int bitIndex);

    boolean get(int index);

    int nextSetBit(int i);

    boolean intersects(ContextSet other);

    ContextSet and(ContextSet other);

    ContextSet or(ContextSet other);

    ContextSet copyOf();

    int size();

}
