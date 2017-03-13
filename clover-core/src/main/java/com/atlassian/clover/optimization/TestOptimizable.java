package com.atlassian.clover.optimization;

import junit.framework.Test;
import junit.framework.TestSuite;
import com.atlassian.clover.api.optimization.Optimizable;

/**
 */
public class TestOptimizable implements Optimizable {


    private final TestSuite test;

    public TestOptimizable(TestSuite test) {
        this.test = test;
    }

    @Override
    public String getName() {
        return test.getName();
    }

    public Test getTest() {
        return test;
    }
}
