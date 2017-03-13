package com.atlassian.clover.spec.instr.test;

import com.atlassian.clover.instr.tests.AndStrategy;
import com.atlassian.clover.instr.tests.BooleanStrategy;

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
