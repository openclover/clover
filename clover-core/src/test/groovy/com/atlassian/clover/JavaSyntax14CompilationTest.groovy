package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.apache.tools.ant.BuildException
import org.junit.Before
import org.junit.Test

import static java.util.regex.Pattern.quote
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.isA
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
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
    void switchStatementWithCaseWithColonCannotUseYield() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchStatementWithCaseColonFailed.java"
        final File srcFile = new File(srcDir, fileName)
        try {
            // it's not allowed to use yield in switch statements (a value returned cannot be ignored)
            // OpenClover grammar parser is simplified and allows 'yield' in place where any statement is allowed
            // but the javac catches this
            instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
            fail("Compilation should have failed")
        } catch (Exception ex) {
            assertThat(ex, isA(BuildException.class))
        }
    }

    @Test
    void switchExpressionWithCaseWithColonCannotUseBreak() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionWithCaseColonFailed.java"
        final File srcFile = new File(srcDir, fileName)
        try {
            // using break in switch expressions is NOT allowed for "case X:" form
            // a reason is that a switch expression must return a value and break returns no value
            // OpenClover is not so restrictive and instrumentation succeeds but javac catches this
            instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)
            fail("Compilation should have failed")
        } catch (Exception ex) {
            assertThat(ex, isA(BuildException.class))
        }
    }

    @Test
    void testSwitchCaseWithColonsMixedWithExpressions() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionMixedCasesFailed.java"
        final File srcFile = new File(srcDir, fileName)
        int returnCode = instrumentSourceFileNoAssert(srcFile, JavaEnvUtils.JAVA_14, [] as String[])

        // it's not allowed to mix "case X:" with "case X ->" in the same switch statement
        // OpenClover's parser should catch this
        assertEquals(1, returnCode)
    }

    @Test
    void switchExpressionWithCaseWithColonCanUseYield() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionWithCaseColon.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // using yield in switch expressions is allowed also for "case X:" form
        // notice there are two R_INC - one for entering the case, one for the statement
        assertFileMatches(fileName, quote("case 0:") + "\\s*" + R_INC + "\\s*" + R_INC + quote("yield 10;"))
        assertFileMatches(fileName, quote("default:") + "\\s*" + R_INC + "\\s*" + R_INC + quote("yield 11;"))
        assertFileMatches(fileName, quote("case 10:") + "\\s*" + R_INC + "\\s*" + R_INC + quote("yield 20;"))
        assertFileMatches(fileName, quote("case 11:") + "\\s*" + R_INC + "\\s*" + R_INC + quote("yield 21;"))
        assertFileMatches(fileName, quote("default:") + "\\s*" + R_INC + "\\s*" + R_INC + quote("yield 22;"))
    }

    @Test
    void switchStatementWithCaseWithColonCanUseBreak() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchStatementWithCaseColon.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // this is a regression test, using break in switch statements must be allowed
        assertFileMatches(fileName, R_INC + quote("k++;") + R_INC + quote("break;"))
        assertFileMatches(fileName, quote("default:") + ".*" + R_INC + quote("break;"))
    }


    @Test
    void switchExpressionWithCaseAndDefaultCanUseLambdasReturningValues() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithLambdas.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // switch expression with value-returning lambdas
        assertFileMatches(fileName, quote("case R ->") + R_CASE_EXPRESSION_LEFT + quote(" \"red\"") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case G ->") + R_CASE_EXPRESSION_LEFT + quote(" \"green\"") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case B ->") + R_CASE_EXPRESSION_LEFT + quote(" \"blue\"") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 0 ->") + R_CASE_EXPRESSION_LEFT + quote(" EvenOrOdd.EVEN") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 1 ->") + R_CASE_EXPRESSION_LEFT + quote(" EvenOrOdd.ODD") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default ->") + R_CASE_EXPRESSION_LEFT + quote(" EvenOrOdd.UNKNOWN") + R_CASE_EXPRESSION_RIGHT)
    }

    @Test
    void switchExpressionWithCaseAndDefaultCanUseLambdasReturningVoid() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithLambdas.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // switch expression with void lambdas
        assertFileMatches(fileName, quote("case R ->") + R_CASE_EXPRESSION_LEFT +
                quote(" System.out.println(\"red\")") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case G ->") + R_CASE_EXPRESSION_LEFT +
                quote(" System.out.println(\"green\")") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case B ->") + R_CASE_EXPRESSION_LEFT +
                quote(" System.out.println(\"blue\")") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 0 ->") + R_CASE_EXPRESSION_LEFT +
                quote(" System.out.println(EvenOrOdd.EVEN)") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 1 ->") + R_CASE_EXPRESSION_LEFT +
                quote(" System.out.println(EvenOrOdd.ODD)") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default ->") + R_CASE_EXPRESSION_LEFT +
                quote(" System.out.println(EvenOrOdd.UNKNOWN)") + R_CASE_EXPRESSION_RIGHT)
    }

    @Test
    void switchExpressionWithCaseAndDefaultCanThrowExceptions() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithThrows.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        assertFileMatches(fileName, quote("case -1 ->") + R_CASE_THROW_EXPRESSION_LEFT +
                quote(" throw new IllegalArgumentException(\"negative\");") + R_CASE_THROW_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default ->") + R_CASE_EXPRESSION_LEFT +
                quote(" 0") + R_CASE_EXPRESSION_RIGHT)

        assertFileMatches(fileName, quote("case -1 ->") + R_CASE_THROW_EXPRESSION_LEFT +
                quote(" throw new IllegalArgumentException(\"negative\");") + R_CASE_THROW_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 0 ->") + R_CASE_THROW_EXPRESSION_LEFT +
                quote(" throw new IllegalArgumentException(\"zero\");") + R_CASE_THROW_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default ->") + R_CASE_THROW_EXPRESSION_LEFT +
                quote(" throw new IllegalArgumentException(\"anything\");") + R_CASE_THROW_EXPRESSION_RIGHT)
    }

    @Test
    void switchExpressionWithBlockWithYield() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithBlocks.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        assertFileMatches(fileName, R_INC + quote("int color = switch (i)"))
        assertFileMatches(fileName, quote("case 0 -> { ") + R_INC + quote("yield 0x00; }"))
        assertFileMatches(fileName, quote("case 1 -> { ") + R_INC + quote("yield 0x10; }"))
        assertFileMatches(fileName, quote("default -> { ") + R_INC + quote("yield 0x20; }"))
    }

    @Test
    void switchExpressionWithBlockReturningVoid() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithBlocks.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        assertFileMatches(fileName, quote("case 0 -> { ") + R_INC + quote("System.out.println(\"0x00\"); }"))
        assertFileMatches(fileName, quote("case 1 -> { ") + R_INC + quote("System.out.println(\"0x10\"); }"))
        assertFileMatches(fileName, quote("default -> { ") + R_INC + quote("System.out.println(\"0xFF\"); }"))
    }

    @Test
    void switchExpressionWithBlockThrowingException() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionCaseAndDefaultWithBlocks.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        assertFileMatches(fileName, R_INC + quote("throw new IllegalArgumentException(\"negative\");"))
        assertFileMatches(fileName, R_INC + quote("throw new IllegalArgumentException(\"positive\");"))
    }

    @Test
    void switchExpressionWithCaseWithMultipleValues() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionWithMultiValueCase.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        assertFileMatches(fileName, quote("case 0, 1, 2*3 ->") + R_CASE_EXPRESSION_LEFT + quote(" 10") + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case 0, 1, 2 ->") + R_CASE_EXPRESSION_LEFT + quote(" 20") + R_CASE_EXPRESSION_RIGHT)
// TODO pattern matching since JDK21
//        assertFileMatches(fileName, quote("case null -> ") + R_CASE_EXPRESSION_LEFT + quote("21;") + R_CASE_EXPRESSION_RIGHT)
//        assertFileMatches(fileName, quote("case null, default -> ") + R_CASE_EXPRESSION_LEFT + quote("31;") + R_CASE_EXPRESSION_RIGHT)
    }

    @Test
    void switchIsAnExpressionInDifferentContexts() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionInVariousContexts.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // assignment to a variable
        assertFileMatches(fileName, R_INC + quote("int k = switch (j) {"))

        // argument of a method call
        assertFileMatches(fileName, R_INC + quote("foo(switch (k) {")) // no R_INC before switch
        assertFileMatches(fileName, quote("case 10 ->") + R_CASE_EXPRESSION_LEFT + " 100" + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default ->") + R_CASE_EXPRESSION_LEFT + " 200" + R_CASE_EXPRESSION_RIGHT)

        // part of an expression
        assertFileMatches(fileName,
                // we have '((((' because of the branch evaluation
                R_INC + quote("if ((((switch (j) {") + "\\s+" +
                quote("case 0 ->") + R_CASE_EXPRESSION_LEFT + quote(" 30") + R_CASE_EXPRESSION_RIGHT + "\\s+" +
                quote("default ->") + R_CASE_EXPRESSION_LEFT + quote(" 31") + R_CASE_EXPRESSION_RIGHT + "\\s+" +
                quote("} % 10 == 0)") +
                quote("&&(") + R_IGET // part of the branch coverage expression
        )
    }

    @Test
    void switchIsAStatementInDifferentContexts() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_14))

        final String fileName = "Java14SwitchExpressionInVariousContexts.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_14)

        // a standalone statement in method
        assertFileMatches(fileName, R_INC + quote("switch (kk) {") + "\\s+" +
                quote("case 77 ->") + R_CASE_EXPRESSION_LEFT +
                quote(" System.out.println(\"77\")") + R_CASE_EXPRESSION_RIGHT)

        // instance initializer block
        assertFileMatches(fileName,
                R_INC + quote("switch (kkk) {") + "\\s+" +
                quote("case 88 ->") + R_CASE_EXPRESSION_LEFT + quote(" System.out.println(\"88\")") + R_CASE_EXPRESSION_RIGHT + "\\s+" +
                quote("default ->") + R_CASE_EXPRESSION_LEFT + quote(" System.out.println(\"not 88\")") + R_CASE_EXPRESSION_RIGHT)
    }

}
