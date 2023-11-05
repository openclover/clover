package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK14
 * b) make sure that when that code is instrumented, it still compiles
 * c) test new language features introduced
 */
class JavaSyntax14CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax14")
        resetAntOutput()
    }

    @Test
    void testCaseWithColonCanUseBothBreakAndYield() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }

    @Test
    void testSwitchWithCaseAndDefaultWithLambdas() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }

    @Test
    void testSwitchWithCaseExpressionAndBlockReturningYield() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }

    @Test
    void testSwitchWithCaseWithMultipleValues() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }

    @Test
    void testCaseExpressionCanBeVoid() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }

    @Test
    void testCaseExpressionCanReturnValue() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }

    @Test
    void testSwitchIsAnExpression() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // assignment to a variable
        // argument of a method call
        // part of an expression
    }

    @Test
    void testSwitchIsAStatement() {
        // a standalone statement in method, instance initializer block, static block
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }

    @Test
    void testSwitchCaseWithColonsMixedWithExpressions() {
        // a standalone statement in method, instance initializer block, static block
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }
}
