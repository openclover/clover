package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static java.util.regex.Pattern.quote
import static org.junit.Assert.assertEquals
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
    void switchExpressionWithCaseWithColonCanUseYield() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionWithCaseColon.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // using yield in switch expressions is allowed also for "case X:" form
        assertFileMatches(fileName, quote("case 0:") + "\\w*" + R_INC + quote("yield 10;"))
        assertFileMatches(fileName, quote("default:") + "\\w*" + R_INC + quote("yield 11;"))
        assertFileMatches(fileName, quote("case 10:") + "\\w*" + R_INC + quote("yield 20;"))
        assertFileMatches(fileName, quote("case 11:") + "\\w*" + R_INC + quote("yield 21;"))
        assertFileMatches(fileName, quote("default:") + "\\w*" + R_INC + quote("yield 22;"))
    }

    @Test
    void switchExpressionWithCaseWithColonCannotUseBreak() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionWithCaseColonFailed.java"
        final File srcFile = new File(srcDir, fileName)
        int returnCode = instrumentSourceFileNoAssert(srcFile, JavaEnvUtils.JAVA_14, [] as String[])

        // using break in switch expressions is NOT allowed for "case X:" form
        // a reason is that a switch expression must return a value and break returns no value
        assertEquals(1, returnCode)
    }

    @Test
    void switchStatementWithCaseWithColonCanUseBreak() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchStatementWithCaseColon.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // this is a regression test, using break in switch statements must be allowed
        assertFileMatches(fileName, quote("case 30:") + "\\w*" + R_INC + quote("k++;") + R_INC + quote("break;"))
        assertFileMatches(fileName, quote("default:") + "\\w*" + R_INC + quote("break;"))
    }

    @Test
    void switchStatementWithCaseWithColonCannotUseYield() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchStatementWithCaseColonFailed.java"
        final File srcFile = new File(srcDir, fileName)
        int returnCode = instrumentSourceFileNoAssert(srcFile, JavaEnvUtils.JAVA_14, [] as String[])

        // it's not allowed to use yield in switch statements (a value returned cannot be ignored)
        assertEquals(1, returnCode)
    }

    @Test
    void switchExpressionWithCaseAndDefaultCanUseLambdasReturningValues() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithLambdas.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // switch expression with value-returning lambdas
        assertFileMatches(fileName, quote("case R -> ") + R_CASE_EXPRESSION_LEFT +  quote("\"red\";") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case G -> ") + R_CASE_EXPRESSION_LEFT +  quote("\"green\";") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case B -> ") + R_CASE_EXPRESSION_LEFT +  quote("\"blue\";") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 0 -> ") + R_CASE_EXPRESSION_LEFT +  quote("EvenOrOdd.EVEN;") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 1 -> ") + R_CASE_EXPRESSION_LEFT +  quote("EvenOrOdd.ODD;") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default -> ") + R_CASE_EXPRESSION_LEFT +  quote("EvenOrOdd.UNKNOWN;") + R_CASE_EXPRESSION_RIGHT)
    }

    @Test
    void switchExpressionWithCaseAndDefaultCanUseLambdasReturningVoid() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithLambdas.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // switch expression with void lambdas
        assertFileMatches(fileName, quote("case R -> ") + R_CASE_EXPRESSION_LEFT_VOID +
                quote("System.out.println(\"red\");") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case G -> ") + R_CASE_EXPRESSION_LEFT_VOID +
                quote("System.out.println(\"green\");") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case B -> ") + R_CASE_EXPRESSION_LEFT_VOID +
                quote("System.out.println(\"blue\");") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 0 -> ") + R_CASE_EXPRESSION_LEFT_VOID +
                quote("System.out.println(EvenOrOdd.EVEN);") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 1 -> ") + R_CASE_EXPRESSION_LEFT_VOID +
                quote("System.out.println(EvenOrOdd.ODD);") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default -> ") + R_CASE_EXPRESSION_LEFT_VOID +
                quote("System.out.println(EvenOrOdd.UNKNOWN);") + R_CASE_EXPRESSION_RIGHT)
    }

    @Test
    void switchExpressionWithCaseAndDefaultCanThrowExceptions() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithThrows.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        assertFileMatches(fileName, quote("case -1 -> ") + R_CASE_EXPRESSION_LEFT +
                quote("throw new IllegalArgumentException(\"negative\");") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default -> ") + R_CASE_EXPRESSION_LEFT +
                quote("0;") + R_CASE_EXPRESSION_RIGHT)

        assertFileMatches(fileName, quote("case -1 -> ") + R_CASE_EXPRESSION_LEFT +
                quote("throw new IllegalArgumentException(\"negative\");") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 0 -> ") + R_CASE_EXPRESSION_LEFT +
                quote("throw new IllegalArgumentException(\"zero\");") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default -> ") + R_CASE_EXPRESSION_LEFT +
                quote("throw new IllegalArgumentException(\"anything\");") + R_CASE_EXPRESSION_RIGHT)
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

        final String fileName = "Java14CaseMixedYieldAndExpression.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
    }
}
