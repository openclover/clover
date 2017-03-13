package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;

public class AlwaysFailingTest extends TestCase {
    public void testFAIL() {
        throw new RuntimeException("FAIL");
    }
}
