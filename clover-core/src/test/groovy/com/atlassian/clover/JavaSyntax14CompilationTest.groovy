package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under JDK14
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax14CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax14")
        resetAntOutput()
    }

    @Test
    void testInstanceOfPatternMatching() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14InstanceOfPatternMatching.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
        assertFileMatches(fileName, R_INC + "System.out.println", false)

        executeMainClasses("Java14InstanceOfPatternMatching")
        assertExecOutputContains("obj is String = a string", false)
        assertExecOutputContains("obj is not null and is an Object and not String or Integer", false)
    }
}
