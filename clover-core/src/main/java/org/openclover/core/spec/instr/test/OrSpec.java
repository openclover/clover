package org.openclover.core.spec.instr.test;

import org.openclover.core.instr.tests.BooleanStrategy;
import org.openclover.core.instr.tests.OrStrategy;

import java.io.Serializable;

public class OrSpec extends BooleanSpec implements Serializable {
    @Override
    public BooleanStrategy getStrategy() {
        return new OrStrategy();
    }

    @Override
    public String toString() {
        return "or(" + super.toString() + ")";
    }
}
