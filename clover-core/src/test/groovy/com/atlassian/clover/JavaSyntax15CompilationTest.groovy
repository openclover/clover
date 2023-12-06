package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.instanceOf
import static org.junit.Assert.fail
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

        final String fileName = "Java15TextBlock.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_15)
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }

    @Test
    void testTextBlockInvalid() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_15))

        final String fileName = "Java15TextBlockInvalid.java"
        try {
            instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_15)
            fail("Expected instrumentation to fail")
        } catch (Exception ex) {
            assertThat(ex, instanceOf(AssertionError.class))
            assertThat(ex.message, containsString("instrumentation problem processing"))
        }
    }
}
