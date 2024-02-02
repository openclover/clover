package org.openclover.idea.testproject;

import junit.framework.TestCase;

import java.util.Date;

public class UniqueTest extends TestCase {
    public void testMethod() {
        A.method(null);
    }

    public void testMethod1() {
        A.method(new Date(2001, 1, 1));
    }

}
