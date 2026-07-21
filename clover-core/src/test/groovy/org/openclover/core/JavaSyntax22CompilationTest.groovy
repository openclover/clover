package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK 22
 * b) make sure that when that code is instrumented, it still compiles and runs.
 * Java 22 finalized JEP 456 - Unnamed Variables & Patterns. The only genuinely new syntax
 * for the parser is the name-only unnamed pattern component '_' inside a record deconstruction
 * (Point(int x, _), Point(_, _)); unnamed variables have always lexed '_' as an IDENT.
 */
class JavaSyntax22CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax22")
        resetAntOutput()
    }

    @Test
    void testUnnamedPatterns() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_22))

        final String fileName = "Java22UnnamedPatterns.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_22)

        // the unnamed '_' pattern component must not be branch-instrumented
        assertFileMatches(fileName, "obj instanceof Point\\(int x, _\\)", false)

        executeMainClasses("Java22UnnamedPatterns")
        assertExecOutputContains("x only = 3", false)
        assertExecOutputContains("y only = 4", false)
        assertExecOutputContains("any point = yes", false)
        assertExecOutputContains("not a point = -1", false)
    }

    @Test
    void testUnnamedVariables() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_22))

        final String fileName = "Java22UnnamedVariables.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_22)

        executeMainClasses("Java22UnnamedVariables")
        assertExecOutputContains("total = 3", false)
        assertExecOutputContains("caught = ok", false)
        assertExecOutputContains("firstOnly = 7", false)
    }
}
