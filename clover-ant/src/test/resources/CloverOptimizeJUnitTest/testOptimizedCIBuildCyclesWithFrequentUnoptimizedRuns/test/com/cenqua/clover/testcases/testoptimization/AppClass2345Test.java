package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;

public class AppClass2345Test extends TestCase {
    public AppClass2345Test(String name) {
        super(name);
    }

    public void testMain() {
        AppClass2.main(null);
        AppClass3.main(null);
        AppClass4.main(null);
        AppClass5.main(null);
    }
}