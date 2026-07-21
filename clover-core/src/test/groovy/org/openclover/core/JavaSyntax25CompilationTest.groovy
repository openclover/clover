package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.util.JavaEnvUtils

import static org.junit.Assume.assumeTrue

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK 25
 * b) make sure that when that code is instrumented, it still compiles and runs.
 *
 * Java 25 finalized three language syntax features:
 *  - JEP 511 Module Import Declarations ('import module M;')
 *  - JEP 512 Compact Source Files and Instance Main Methods
 *  - JEP 513 Flexible Constructor Bodies (statements before super()/this())
 */
class JavaSyntax25CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax25")
        resetAntOutput()
    }

    @Test
    void testModuleImportDeclaration() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_25))

        final String fileName = "Java25ModuleImport.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_25)

        executeMainClasses("Java25ModuleImport")
        assertExecOutputContains("size = 2", false)
    }

    @Test
    void testOrdinaryImportWithModulePrefix() {
        // Instrument-only (parse) check: an ordinary import whose first path segment is 'module'
        // must not be mistaken for a module import. The imported type is fictional, so the file
        // is not compiled/run - only parsing matters for the disambiguation.
        final String fileName = "Java25OrdinaryImportModulePrefix.java"
        final File srcFile = new File(srcDir, fileName)
        instrumentSourceFile(srcFile, JavaEnvUtils.JAVA_25)

        // the ordinary import survives as a normal import (parsed, not rewritten as a module import)
        assertFileMatches(fileName, "import module\\.something\\.Widget;", false)
    }

    @Test
    void testFlexibleConstructorBody() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_25))

        final String fileName = "Java25FlexibleConstructor.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_25)

        executeMainClasses("Java25FlexibleConstructor")
        assertExecOutputContains("p1 = Alice 42", false)
        assertExecOutputContains("p2 = Bob 30", false)
    }

    @Test
    void testCompactSourceFileWithInstanceMain() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_25))

        final String fileName = "Java25CompactSourceFile.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_25)

        executeMainClasses("Java25CompactSourceFile")
        assertExecOutputContains("greeting = Hello", false)
        assertExecOutputContains("doubled = 42", false)
    }

    /**
     * A file with a top-level method is a COMPACT source file even when it also contains a top-level
     * type declaration (the type becomes a member class of the implicit class). Asserts the
     * file is treated as compact (recorder injected as a top-level member) and that the member class's
     * method is instrumented against the same top-level recorder, then compiles and runs.
     */
    @Test
    void testCompactSourceFileWithTypeDeclaration() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_25))

        final String fileName = "Java25CompactWithType.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_25)

        // treated as compact: the recorder inner class is injected as a top-level member...
        assertFileMatches(fileName, "public static class __CLR[a-zA-Z0-9_]+\\{", false)
        // ...and the member class 'Box' method is instrumented against that same top-level recorder
        assertFileMatches(fileName, "(?s)int count\\(\\) \\{" + R_INC, false)

        executeMainClasses("Java25CompactWithType")
        assertExecOutputContains("count = 2", false)
        assertExecOutputContains("first = <a>", false)
    }

    /**
     * Java 25+: BOTH inc() calls for a constructor land BEFORE the explicit super()/this() invocation
     * - the method-entry inc() right after '{', and the invocation's own statement inc() immediately
     * before super()/this().
     */
    @Test
    void testConstructorEntryAndStatementIncPlacedBeforeSuperAndThisAtJava25() {
        final String fileName = "Java25ConstructorPlacement.java"
        final File srcFile = new File(srcDir, fileName)
        instrumentSourceFile(srcFile, JavaEnvUtils.JAVA_25)

        // two inc() calls (method-entry + statement) sit between '{' and super()/this()
        assertFileMatches(fileName, "(?s)\\{\\s*" + R_INC + "\\s*" + R_INC + "\\s*super\\(x\\)", false)
        assertFileMatches(fileName, "(?s)\\{\\s*" + R_INC + "\\s*" + R_INC + "\\s*this\\(x, 0\\)", false)

        // ...so the explicit invocation is no longer the first thing after '{' (it was, pre-25)
        assertFileMatches(fileName, "(?s)\\{\\s*super\\(x\\)", true)
        assertFileMatches(fileName, "(?s)\\{\\s*this\\(x, 0\\)", true)

        // sanity check: with the entry inc() in the prologue the instrumented source still compiles
        // and runs - but only where a JDK 25 is available (the placement assertions above run on any JDK)
        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_25)) {
            compileSources(mGenSrcDir, [ fileName ] as String[], JavaEnvUtils.JAVA_25)
            executeMainClasses("Java25ConstructorPlacement")
            assertExecOutputContains("a.y = 2", false)
            assertExecOutputContains("b.y = 0", false)
        }
    }

    /**
     * At source levels below 25 both the method-entry inc() and the invocation's
     * statement inc() must remain in their classic position - AFTER super()/this() - since
     * statements before the explicit invocation were illegal there.
     */
    @Test
    void testConstructorEntryAndStatementIncPlacedAfterSuperAndThisBeforeJava25() {
        final String fileName = "Java25ConstructorPlacement.java"
        // instrument AND compile at source 21 - the classic-constructor fixture builds on any JDK
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_21)

        // the explicit super()/this() invocation is the first thing after '{' (no prologue inc())
        assertFileMatches(fileName, "(?s)\\{\\s*super\\(x\\)", false)
        assertFileMatches(fileName, "(?s)\\{\\s*this\\(x, 0\\)", false)

        // ...both inc() calls trail the explicit invocation
        assertFileMatches(fileName, "(?s)super\\(x\\);\\s*" + R_INC + "\\s*" + R_INC, false)
        assertFileMatches(fileName, "(?s)this\\(x, 0\\);\\s*" + R_INC + "\\s*" + R_INC, false)

        // sanity check that the instrumented (<25) source actually runs
        executeMainClasses("Java25ConstructorPlacement")
        assertExecOutputContains("a.y = 2", false)
        assertExecOutputContains("b.y = 0", false)
    }
}
