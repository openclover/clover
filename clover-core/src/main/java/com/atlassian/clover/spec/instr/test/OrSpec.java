package com.atlassian.clover.spec.instr.test;

import com.atlassian.clover.instr.tests.BooleanStrategy;
import com.atlassian.clover.instr.tests.OrStrategy;

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
