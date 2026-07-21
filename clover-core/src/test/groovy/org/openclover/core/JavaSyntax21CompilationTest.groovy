package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static java.util.regex.Pattern.quote
import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK21
 * b) make sure that when that code is instrumented, it still compiles and runs.
 * Java 21 finalized two language syntax features:
 *  - JEP 441 Pattern Matching for switch (type patterns, guarded 'when', 'case null')
 *  - JEP 440 Record Patterns (deconstruction in switch and instanceof)
 * The instrumentation assertions below intentionally do NOT check that the original source line is
 * merely present. Instead they verify that the recorder calls were (or, for pattern bindings, were
 * NOT) injected in the right place.
 */
class JavaSyntax21CompilationTest extends JavaSyntaxCompilationTestBase {

    /** Regular expression for the fall-through guard flag: __CLB_hash_code */
    protected final String CLB_FLAG = "__CLB[a-zA-Z0-9_]+"

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

        // arrow-form case bodies are wrapped as: case Integer i ->{R.inc(n);yield  "int " + i;}
        assertFileMatches(fileName, quote("case Integer i ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"int " + i;') + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case String s ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"str " + s;') + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("default ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"other " + obj;') + R_CASE_EXPRESSION_RIGHT)

        // the type pattern binding variable must not be instrumented at all - neither a branch nor a
        // statement increment may be injected into the 'Type binding' declaration
        assertFileMatches(fileName, quote("case Integer i ->") + ".*" + R_IGET, true)
        assertFileMatches(fileName, quote("case String s ->") + ".*" + R_IGET, true)
        assertFileMatches(fileName, quote("case ") + R_INC, true)
        assertFileMatches(fileName, quote("case Integer {"), true)

        // colon-form case labels get a fall-through hit flag, and each case body statement is incremented
        assertFileMatches(fileName, quote("case Integer i:if (!") + CLB_FLAG + quote(") {") + R_INC)
        assertFileMatches(fileName, quote("case String s:if (!") + CLB_FLAG + quote(") {") + R_INC)
        assertFileMatches(fileName, R_INC + quote('return "int " + i;'))
        assertFileMatches(fileName, R_INC + quote('return "str " + s;'))
        // colon-form binding variable must not be branch-instrumented either
        assertFileMatches(fileName, quote("case Integer i:") + ".*" + R_IGET, true)

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

        // the 'when' guard is a boolean expression and IS branch-instrumented (true/false coverage),
        // exactly like an 'if' condition - see the inline (iget!=0|true)/(iget==0&false) wrapper
        assertFileMatches(fileName, quote("when (((i > 10 )&&") + R_IGET_TRUE + quote(")||") + R_IGET_FALSE + quote(")->"))
        assertFileMatches(fileName, quote("when (((s.length() > 3 )&&") + R_IGET_TRUE + quote(")||") + R_IGET_FALSE + quote(")->"))

        // the case body on the right-hand side of the arrow is still wrapped with an increment
        assertFileMatches(fileName, quote("->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"big int " + i;') + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case Integer i ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"small int " + i;') + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"long str " + s;') + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case null ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"null";') + R_CASE_EXPRESSION_RIGHT)

        // the pattern binding declaration must NEVER be instrumented - no increment or branch is
        // injected into the 'Type binding' part of the label
        assertFileMatches(fileName, quote("case ") + R_INC, true)
        assertFileMatches(fileName, quote("case Integer i ") + R_IGET, true)
        assertFileMatches(fileName, quote("case Integer {"), true)

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

        // 'case null' and the combined 'case null, default' both get their body incremented
        assertFileMatches(fileName, quote("case null ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"was null";') + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case null, default ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"null or other";') + R_CASE_EXPRESSION_RIGHT)

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

        // the enclosing 'if' statement is instrumented...
        assertFileMatches(fileName, R_INC + quote("if (obj instanceof Point(int x, int y))"))
        // ...but the record deconstruction pattern must not be branch-instrumented
        assertFileMatches(fileName, quote("obj instanceof Point(int x, int y)") + ".*" + R_IGET, true)
        // and the guarded body statement is incremented
        assertFileMatches(fileName, R_INC + quote("return x + y;"))

        executeMainClasses("Java21RecordPatternInstanceOf")
        assertExecOutputContains("sum = 7", false)
        assertExecOutputContains("not a point = -1", false)
    }

    @Test
    void testRecordPatternWithGuard() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))

        final String fileName = "Java21RecordPatternGuard.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        // the 'when' guard following a record deconstruction pattern IS branch-instrumented
        assertFileMatches(fileName, quote("when (((x + y > 0 )&&") + R_IGET_TRUE + quote(")||") + R_IGET_FALSE + quote(")->"))
        // both the guarded and the un-guarded case bodies are incremented on the right of the arrow
        assertFileMatches(fileName, quote("->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"positive sum";') + R_CASE_EXPRESSION_RIGHT)
        assertFileMatches(fileName, quote("case Point(int x, int y) ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"non-positive sum";') + R_CASE_EXPRESSION_RIGHT)
        // the record deconstruction bindings (x, y) must not be branch-instrumented
        assertFileMatches(fileName, quote("Point(") + R_IGET, true)
        assertFileMatches(fileName, quote("case ") + R_INC, true)

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

        // nested record deconstruction case bodies are incremented
        assertFileMatches(fileName, quote("case Line(Point(var x1, var y1), Point(var x2, var y2)) ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT)
        assertFileMatches(fileName, quote("case Point(var x, var y) ->") + R_CASE_EXPRESSION_WITH_YIELD_LEFT + " *" + quote('"point (" + x'))
        // the nested binding variables must not be branch-instrumented
        assertFileMatches(fileName, quote("case Line(Point(var x1, var y1), Point(var x2, var y2)) ->") + ".*" + R_IGET, true)

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
        assertFileMatches(fileName, R_INC + quote("this.when = when;"))
        assertFileMatches(fileName, R_INC + quote("return when;"))
        assertFileMatches(fileName, R_INC + quote("int when = obj.when(10);"))

        executeMainClasses("Java21WhenKeyword")
        assertExecOutputContains("result = 10", false)
    }
}
