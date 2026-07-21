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
}
