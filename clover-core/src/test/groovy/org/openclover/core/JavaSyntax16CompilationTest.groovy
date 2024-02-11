package org.openclover.core

import org.openclover.core.util.JavaEnvUtils
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


    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax16")
        resetAntOutput()
    }

    @Test
    void testRecordClass() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_16))

        final String fileName = "Java16RecordClass.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_16)

        // Record2 constructor is instrumented
        assertFileMatches(fileName, "public Record2\\(int x, int y\\) \\{" + R_INC, false)
        assertFileMatches(fileName, R_INC + "this\\.x = x \\* 2", false)
        assertFileMatches(fileName, R_INC + "this\\.y = y \\* 2", false)

        // Record3 method is instrumented
        assertFileMatches(fileName, R_INC + "return x \\+ y \\+ z;", false)

        // RecordIsNotAReservedKeyword method with 'record' as symbols is instrumented
        assertFileMatches(fileName, R_INC + "this\\.record = record;", false)
        assertFileMatches(fileName, R_INC + "System\\.out\\.println\\(record\\);", false)
        assertFileMatches(fileName, R_INC + "return record;", false)

        // CompactConstructor constructor is instrumented
        assertFileMatches(fileName, "CompactConstructor \\{" + R_INC, false)
        assertFileMatches(fileName, R_INC + "a \\*= 10;", false)
        assertFileMatches(fileName, R_INC + "b \\+= 2;", false)
    }

    @Test
    void testRecordIsNotReservedKeyword() {
        // test under every JDK to ensure there's no regression (bug OC-206)
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.7")
        final String fileName = "RecordIsNotReservedKeyword.java"

        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        assertFileMatches(fileName, R_INC + "System\\.out\\.println\\(record\\);", false)
    }

    @Test
    void expectCodeInstrumentsAndCompilesAndRunsWithInstanceOfPatternMatching() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_16))

        final String fileName = "Java16InstanceOfPatternMatching.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_16)
        assertFileMatches(fileName, R_INC + "System\\.out\\.println\\(\"obj is String", false)
        assertFileMatches(fileName, R_INC + "System\\.out\\.println\\(\"obj is final String", false)

        executeMainClasses("Java16InstanceOfPatternMatching")
        assertExecOutputContains("obj is String = a string", false)
        assertExecOutputContains("obj is final String = a final string", false)
        assertExecOutputContains("obj is not null and is an Object and not String or Integer", false)
    }

    @Test
    void expectNoBranchInstrumentationForInstanceOfWithPatternMatching() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_16))

        final String fileName = "Java16InstanceOfPatternMatching.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_16)

        assertFileMatches(fileName, "\\(o1 instanceof String\\)&&\\(" + R_IGET, false) // instrument
        assertFileMatches(fileName, "\\(o2 instanceof String s\\)", false) // do not instrument
        assertFileMatches(fileName, "\\(o3 instanceof A\\.B ab\\)", false) // do not instrument
        assertFileMatches(fileName, "\\(o4 instanceof A\\.B\\[\\] arr\\)", false) // do not instrument
    }
}
