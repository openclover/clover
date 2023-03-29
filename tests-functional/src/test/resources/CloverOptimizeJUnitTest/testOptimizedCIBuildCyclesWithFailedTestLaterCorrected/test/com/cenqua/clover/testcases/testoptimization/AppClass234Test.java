package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;

public class AppClass234Test extends TestCase {
    public AppClass234Test(String name) {
        super(name);
    }

    public void testMain() {
        AppClass2.main(null);
        AppClass3.main(null); 
        AppClass4.main(null);
    }
}