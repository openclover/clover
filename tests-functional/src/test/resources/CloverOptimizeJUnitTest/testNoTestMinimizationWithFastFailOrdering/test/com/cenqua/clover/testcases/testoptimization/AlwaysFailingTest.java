package com.cenqua.clover.testcases.testoptimization;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class AlwaysFailingTest extends TestCase {
    public AlwaysFailingTest(String name) {
        super(name);
    }

    public void testFAIL() throws Exception {
        final File file = new File(System.getProperty("outdir"), "testorder.log");
        if (!file.exists()) {
            file.createNewFile();
        }
        final PrintWriter pw = new PrintWriter(new FileWriter(file, true));
        pw.println("AlwaysFailingTest");
        pw.close();

        fail();
    }
}
