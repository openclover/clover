package com.cenqua.clovertest;

import junit.framework.TestCase;

public class FailingTest extends TestCase {

    public void testFailing() {
        assertTrue("This one should fail", false);
    }
}
