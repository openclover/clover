package org.openclover.core.optimization;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.openclover.core.api.optimization.Optimizable;

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
