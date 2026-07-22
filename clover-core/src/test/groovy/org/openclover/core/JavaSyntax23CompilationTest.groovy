package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK 23
 * b) make sure that when that code is instrumented, it still compiles and runs.
 * Java 23 finalized JEP 467 - Markdown Documentation Comments ('///'). This needs no grammar
 * change: the lexer already treats '///' as an ordinary line comment and discards it. The test
 * only guards against regressions and confirms the '23' source level is accepted.
 */
class JavaSyntax23CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax23")
        resetAntOutput()
    }

    @Test
    void testMarkdownDocComments() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_23))

        final String fileName = "Java23MarkdownDoc.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_23)

        executeMainClasses("Java23MarkdownDoc")
        assertExecOutputContains("sum = 5", false)
    }

    @Test
    void testSourceLevelAcceptedAndInstruments() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_23))

        final String fileName = "Java23Simple.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_23)

        executeMainClasses("Java23Simple")
        assertExecOutputContains("value = 42", false)
    }
}
