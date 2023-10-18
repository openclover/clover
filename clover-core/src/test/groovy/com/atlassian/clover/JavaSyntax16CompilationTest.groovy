package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK16
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax16CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax16")
        resetAntOutput()
    }

    @Test
    void testRecordClass() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_16))

        final String fileName = "java16/Java16RecordClass.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_16)
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }

    @Test
    void testRecordIsNotReservedKeyword() {
        // test under every JDK to ensure there's no regression (bug OC-206)
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.7")
        final String fileName = "RecordIsNotReservedKeyword.java"

        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        assertFileMatches(fileName, R_INC + "System.out.println(record)", false)
    }
}
