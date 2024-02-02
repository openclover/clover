package org.openclover.idea.testproject;

import junit.framework.TestCase;

public class FailingTest extends TestCase {

    public void testFailing() {
        assertTrue("This one should fail", false);
    }
}
