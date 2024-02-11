package org.openclover.core.spec.instr.test;

import org.openclover.core.instr.tests.AndStrategy;
import org.openclover.core.instr.tests.BooleanStrategy;

import java.io.Serializable;

public class AndSpec extends BooleanSpec implements Serializable {
    @Override
    public BooleanStrategy getStrategy() {
        return new AndStrategy();
    }

    @Override
    public String toString() {
        return "and(" + super.toString() + ")";
    }
}
