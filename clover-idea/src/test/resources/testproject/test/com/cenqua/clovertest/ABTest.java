package com.cenqua.clovertest;

import junit.framework.TestCase;

public class ABTest extends TestCase {
    public void testAB() {
        A a = new A();
        a.a1();
        a.a2();

        new B().b1();
    }

}
