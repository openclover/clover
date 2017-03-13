package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;

public class AppClass23Test extends TestCase {
    public AppClass23Test(String name) {
        super(name);
    }

    public void testMain() {
        AppClass2.main(null);
        AppClass3.main(null);
    }
}