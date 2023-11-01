package com.atlassian.clover

import com.atlassian.clover.util.FileUtils
import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assume.assumeTrue


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK9
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax9CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax9")
        resetAntOutput()
    }

    @Test
    void testAnnotationsOnJavaTypes() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9))

        final String fileName = "java9/Java9PrivateInterfaceMethod.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_9)

        // check private methods in interfaces are instrumented
        assertFileMatches(fileName, R_INC + "int i = 0;", false)
    }

    @Test
    void testModuleInfoInstrumentation() {
        // copy sub-packages - we're not interested in instrumenting them
        FileUtils.dirCopy(srcDir, mGenSrcDir, true);
        // instrument just module-info.java
        File moduleInfo = new File(srcDir, "module-info.java");
        instrumentSourceFile(moduleInfo, JavaEnvUtils.JAVA_9)
    }

    @Test
    void testModuleInfoInstrumentationAndCompilation() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9))

        // copy sub-packages - we're not interested in instrumenting them
        FileUtils.dirCopy(srcDir, mGenSrcDir, true);
        // instrument just module-info.java
        File moduleInfo = new File(srcDir, "module-info.java");
        instrumentSourceFile(moduleInfo, JavaEnvUtils.JAVA_9)
        // compile all stuff
        compileSources(mGenSrcDir, [ "module-info.java"] as String[], JavaEnvUtils.JAVA_9)
        // expect no instrumentation in module-info.java
        assertFileMatches("module-info.java", R_INC, true)
    }

    @Test
    void testDoesNotFailOnModuleInfoKeywordsInRegularSourceFile() {
        File sourceFile = new File(new File(srcDir, "java9"), "NonModuleInfo.java");
        instrumentSourceFile(sourceFile, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTryWithResourcesWithVariable() {
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9))

        final String fileName = "java9/Java9TryWithResourcesWithVariable.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_9)

        assertFileMatches(fileName, R_INC + "ps.println", false)
    }
}
