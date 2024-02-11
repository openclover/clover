package org.openclover.core.instr.tests;

import java.io.Serializable;

public class OrStrategy implements BooleanStrategy, Serializable {
    @Override
    public boolean process(boolean[] values) {
        boolean result = false;
        for (int i = 0; i < values.length && !result; i++) { // break as soon as one value is true
            result = (result || values[i]);
        }
        return result;
    }
}
