package com.atlassian.clover


import org.junit.Before
import org.junit.Test


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK16
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax116CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax1.16")
        resetAntOutput()
    }

    @Test
    void testRecordClass() {
        final String fileName = "java16/Java16RecordClass.java"
        File srcFile = new File(srcDir, fileName)

        // Currently, OpenClover cannot be built on JDK16
        instrumentSourceFile(srcFile,  com.atlassian.clover.util.JavaEnvUtils.JAVA_16)
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }
}
