package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK10
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax10CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax10")
        resetAntOutput()
    }

    @Test
    void testVarVariable() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_10))

        final String fileName = "java10/Java10Var.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_10)

        // check private methods in interfaces are instrumented
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }
}
