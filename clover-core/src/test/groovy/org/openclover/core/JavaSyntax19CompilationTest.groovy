package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure Clover accepts '--source 19'
 * b) make sure an ordinary Java 19 class is instrumented and still compiles.
 * Java 19 introduced no finalized language syntax, so there is no feature to exercise here.
 */
class JavaSyntax19CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax19")
        resetAntOutput()
    }

    @Test
    void testSourceLevelAcceptedAndInstruments() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_19))

        final String fileName = "Java19Simple.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_19)
        assertFileMatches(fileName, R_INC + "return a \\+ b;", false)

        executeMainClasses("Java19Simple")
        assertExecOutputContains("Java19Simple sum = 5", false)
    }
}
