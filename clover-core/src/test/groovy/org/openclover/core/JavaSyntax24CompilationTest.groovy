package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK 24
 * b) make sure that when that code is instrumented, it still compiles and runs.
 *
 * Java 24 adds no finalized language syntax (all its language JEPs are preview iterations), so
 * SourceLevel.JAVA_24 is a pure version pass-through. This test only confirms '24' is accepted.
 */
class JavaSyntax24CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax24")
        resetAntOutput()
    }

    @Test
    void testSourceLevelAcceptedAndInstruments() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_24))

        final String fileName = "Java24Simple.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_24)

        executeMainClasses("Java24Simple")
        assertExecOutputContains("value = 24", false)
    }
}
