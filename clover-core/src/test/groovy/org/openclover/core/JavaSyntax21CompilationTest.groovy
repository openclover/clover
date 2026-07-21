package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK21
 * b) make sure that when that code is instrumented, it still compiles and runs.
 * Java 21 finalized two language syntax features:
 *  - JEP 441 Pattern Matching for switch (type patterns, guarded 'when', 'case null')
 *  - JEP 440 Record Patterns (deconstruction in switch and instanceof)
 */
class JavaSyntax21CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax21")
        resetAntOutput()
    }

    @Test
    void testSwitchTypePatterns() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21SwitchTypePatterns.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        // the type pattern binding variable must not be branch-instrumented
        assertFileMatches(fileName, "case Integer i ->", false)
        assertFileMatches(fileName, "case String s:", false)

        executeMainClasses("Java21SwitchTypePatterns")
        assertExecOutputContains("arrow Integer = int 42", false)
        assertExecOutputContains("arrow String = str hello", false)
        assertExecOutputContains("arrow Object = other 3.14", false)
        assertExecOutputContains("colon Integer = int 7", false)
        assertExecOutputContains("colon String = str world", false)
        assertExecOutputContains("colon Object = other 2.71", false)
    }

    @Test
    void testSwitchGuardedPattern() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21SwitchGuardedPattern.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        executeMainClasses("Java21SwitchGuardedPattern")
        assertExecOutputContains("5 -> small int 5", false)
        assertExecOutputContains("50 -> big int 50", false)
        assertExecOutputContains("hi -> short str hi", false)
        assertExecOutputContains("null -> null", false)
    }

    @Test
    void testSwitchNullCase() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21SwitchNullCase.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        executeMainClasses("Java21SwitchNullCase")
        assertExecOutputContains("null -> was null", false)
        assertExecOutputContains("abc -> str abc", false)
        assertExecOutputContains("null combined -> null or other", false)
        assertExecOutputContains("xyz combined -> null or other", false)
    }

    @Test
    void testRecordPatternInstanceof() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21RecordPatternInstanceOf.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        // the record deconstruction pattern must not be branch-instrumented
        assertFileMatches(fileName, "\\(obj instanceof Point\\(int x, int y\\)\\)", false)

        executeMainClasses("Java21RecordPatternInstanceOf")
        assertExecOutputContains("sum = 7", false)
        assertExecOutputContains("not a point = -1", false)
    }

    @Test
    void testRecordPatternWithGuard() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21RecordPatternGuard.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        executeMainClasses("Java21RecordPatternGuard")
        assertExecOutputContains("\\(3,4\\) -> positive sum", false)
        assertExecOutputContains("\\(0,0\\) -> non-positive sum", false)
        assertExecOutputContains("other -> not a point", false)
    }

    @Test
    void testNestedRecordPattern() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21NestedRecordPattern.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        executeMainClasses("Java21NestedRecordPattern")
        assertExecOutputContains("describe = line \\(0,0\\)->\\(3,4\\)", false)
        assertExecOutputContains("describe point = point \\(1,2\\)", false)
        assertExecOutputContains("describe other = other", false)
    }

    @Test
    void testWhenIsNotReservedKeyword() {
        // 'when' is only a contextual keyword and must remain usable as an identifier
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21WhenKeyword.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)
        assertFileMatches(fileName, R_INC + "this\\.when = when;", false)
        assertFileMatches(fileName, R_INC + "return when;", false)

        executeMainClasses("Java21WhenKeyword")
        assertExecOutputContains("result = 10", false)
    }
}
