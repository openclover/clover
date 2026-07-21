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
     * JEP 513 placement (Java 25+): BOTH inc() calls for a constructor land BEFORE the explicit
     * super()/this() invocation - the method-entry inc() right after '{', and the invocation's own
     * statement inc() immediately before super()/this() (so a throw while evaluating its arguments
     * still records the statement as entered, like every other statement). These are instrument-only
     * (parse+rewrite) assertions using a classic constructor fixture, so they run on any JDK - the
     * placement is gated purely by the '25' source level (FLEXIBLE_CONSTRUCTORS), not the runtime JDK.
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
    }

    /**
     * Regression: at source levels below 25 both the method-entry inc() and the invocation's
     * statement inc() must remain in their classic position - AFTER super()/this() - since
     * statements before the explicit invocation were illegal there.
     */
    @Test
    void testConstructorEntryAndStatementIncPlacedAfterSuperAndThisBeforeJava25() {
        final String fileName = "Java25ConstructorPlacement.java"
        final File srcFile = new File(srcDir, fileName)
        instrumentSourceFile(srcFile, JavaEnvUtils.JAVA_21)

        // the explicit super()/this() invocation is the first thing after '{' (no prologue inc())
        assertFileMatches(fileName, "(?s)\\{\\s*super\\(x\\)", false)
        assertFileMatches(fileName, "(?s)\\{\\s*this\\(x, 0\\)", false)

        // ...both inc() calls trail the explicit invocation
        assertFileMatches(fileName, "(?s)super\\(x\\);\\s*" + R_INC + "\\s*" + R_INC, false)
        assertFileMatches(fileName, "(?s)this\\(x, 0\\);\\s*" + R_INC + "\\s*" + R_INC, false)
    }
}
