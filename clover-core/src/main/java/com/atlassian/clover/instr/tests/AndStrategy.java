package com.atlassian.clover.instr.tests;

import java.io.Serializable;

public class AndStrategy implements BooleanStrategy, Serializable {

    @Override
    public boolean process(boolean[] values) {
        boolean result = true;
        for (int i = 0; i < values.length && result; i++) {
            result = (result && values[i]);
        }
        return result && values.length > 0;
    }
}