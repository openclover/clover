package org.openclover.core

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK11
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax11CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax11")
        resetAntOutput()
    }

    @Test
    void testVarVariableInLambdaParameter() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_11))

        final String fileName = "java11/Java11VarInLambdaParameter.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_11)

        // check private methods in interfaces are instrumented
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }
}
