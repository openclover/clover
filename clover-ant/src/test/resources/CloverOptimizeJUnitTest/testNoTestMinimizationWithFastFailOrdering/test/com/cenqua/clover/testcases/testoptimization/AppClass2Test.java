package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;
import java.io.*;

public class AppClass2Test extends TestCase {
    public AppClass2Test(String name) {
        super(name);
    }

    public void testMain() throws Exception {
        final File file = new File(System.getProperty("outdir"), "testorder.log");
        if (!file.exists()) {
            file.createNewFile();
        }
        final PrintWriter pw = new PrintWriter(new FileWriter(file, true));
        pw.println("AppClass2Test");
        AppClass2.main(null);
        pw.close();
        Thread.sleep(1500);
    }
}