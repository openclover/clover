package org.openclover.core

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK17
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax17CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax17")
        resetAntOutput()
    }

    @Test
    void testSealedClassesParsing() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_17))

        final String fileName = "Java17SealedClass.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_17)

        // assertFileMatches(fileName, R_INC + "return x \\+ y \\+ z;", false)
    }

    @Test
    void testSealedInterfacesParsing() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_17))

        final String fileName = "Java17SealedInterface.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_17)

        // assertFileMatches(fileName, R_INC + "return x \\+ y \\+ z;", false)
    }

    @Test
    void testSealedNonSealedPermitsKeywordsInOtherContexts() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_17))

        final String fileName = "Java17SealedKeywords.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_17)

        // assertFileMatches(fileName, R_INC + "return x \\+ y \\+ z;", false)
    }

}
