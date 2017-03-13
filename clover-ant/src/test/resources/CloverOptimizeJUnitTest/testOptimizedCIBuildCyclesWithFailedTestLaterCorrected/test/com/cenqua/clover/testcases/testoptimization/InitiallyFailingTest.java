package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;

public class InitiallyFailingTest extends TestCase {
    public InitiallyFailingTest(String name) {
        super(name);
    }

    public void testMain() {
        if ("cycle-0".equals(System.getProperty("cycletag"))) {
            fail();
        }
    }
}