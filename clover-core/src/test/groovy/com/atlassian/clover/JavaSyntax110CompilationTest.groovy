package com.atlassian.clover


import org.apache.tools.ant.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK10
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax110CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax1.10")
        resetAntOutput()
    }

    @Test
    void testVarVariable() {
        if (JavaEnvUtils.isAtLeastJavaVersion(com.atlassian.clover.util.JavaEnvUtils.JAVA_10)) {
            final String fileName = "java10/Java10Var.java"
            instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, com.atlassian.clover.util.JavaEnvUtils.JAVA_10)

            // check private methods in interfaces are instrumented
            assertFileMatches(fileName, R_INC + "System.out.println", false)
        }
    }
}
