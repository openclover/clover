package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure Clover accepts '--source 18' (SourceLevel wiring, see OC-229)
 * b) make sure an ordinary Java 18 class is instrumented and still compiles.
 *
 * Java 18 introduced no permanent language syntax, so there is no feature to exercise here.
 */
class JavaSyntax18CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax18")
        resetAntOutput()
    }

    @Test
    void testSourceLevelAcceptedAndInstruments() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_18))

        final String fileName = "Java18Simple.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_18)
        assertFileMatches(fileName, R_INC + "return a \\+ b;", false)

        executeMainClasses("Java18Simple")
        assertExecOutputContains("Java18Simple sum = 5", false)
    }
}
