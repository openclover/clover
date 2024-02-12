package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class AppClass234Test extends TestCase {
    public AppClass234Test(String name) {
        super(name);
    }

    public void testMain() throws Exception {
        final File file = new File(System.getProperty("outdir"), "testorder.log");
        if (!file.exists()) {
            file.createNewFile();
        }
        final PrintWriter pw = new PrintWriter(new FileWriter(file, true));
        pw.println("AppClass234Test");
        AppClass2.main(null);
        AppClass3.main(null);
        AppClass4.main(null);
        pw.close();
        Thread.sleep(500);
    }
}