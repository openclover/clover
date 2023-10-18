package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK15
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax15CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax1.15")
        resetAntOutput()
    }

    @Test
    void testTextBlock() {
        final String fileName = "java15/Java15TextBlock.java"
        File srcFile = new File(srcDir, fileName)

        // Currently, OpenClover cannot be built on JDK15
        instrumentSourceFile(srcFile, JavaEnvUtils.JAVA_15)
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }
}
