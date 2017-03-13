package com.cenqua.clovertest;

import junit.framework.TestCase;

public class AOnlyTest extends TestCase {

    public void testA1() throws InterruptedException {
        new A().a1();
        Thread.sleep(100);
    }

    public void testA2() {
        new A().a2();
    }

    public void testA3() throws InterruptedException {
        new A().a2();
        Thread.sleep(200);
    }

    public void testA99() throws InterruptedException {
        Thread.sleep(300);
    }

}
