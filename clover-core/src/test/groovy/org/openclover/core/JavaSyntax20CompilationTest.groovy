package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure Clover accepts '--source 20' (SourceLevel wiring, see OC-229)
 * b) make sure an ordinary Java 20 class is instrumented and still compiles.
 *
 * Java 20 introduced no finalized language syntax, so there is no feature to exercise here.
 */
class JavaSyntax20CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax20")
        resetAntOutput()
    }

    @Test
    void testSourceLevelAcceptedAndInstruments() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_20))

        final String fileName = "Java20Simple.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_20)
        assertFileMatches(fileName, R_INC + "return a \\+ b;", false)

        executeMainClasses("Java20Simple")
        assertExecOutputContains("Java20Simple sum = 5", false)
    }
}
