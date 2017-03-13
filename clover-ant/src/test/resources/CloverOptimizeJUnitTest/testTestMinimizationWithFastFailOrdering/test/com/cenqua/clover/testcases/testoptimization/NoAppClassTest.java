package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;
import java.io.*;

public class NoAppClassTest extends TestCase {
    public NoAppClassTest(String name) {
        super(name);
    }

    public void testMain() throws Exception {
        final File file = new File(System.getProperty("outdir"), "testorder.log");
        if (!file.exists()) {
            file.createNewFile();
        }
        final PrintWriter pw = new PrintWriter(new FileWriter(file, true));
        pw.println("NoAppClassTest");
        pw.close();
        Thread.sleep(1000);
    }
}