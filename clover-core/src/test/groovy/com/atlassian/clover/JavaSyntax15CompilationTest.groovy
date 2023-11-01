package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK15
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax15CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax15")
        resetAntOutput()
    }

    @Test
    void testTextBlock() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_15))

        final String fileName = "java15/Java15TextBlock.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_15)
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }
}
