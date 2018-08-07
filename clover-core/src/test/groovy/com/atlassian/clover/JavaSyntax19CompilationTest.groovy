package com.atlassian.clover

import com.atlassian.clover.util.FileUtils
import org.apache.tools.ant.util.JavaEnvUtils


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK9
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax19CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    @Override
    protected void setUp() throws Exception {
        super.setUp()
        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_9)) {
            srcDir = new File(mTestcasesSrcDir, "javasyntax1.9")
            resetAntOutput()
        }
    }

    void testAnnotationsOnJavaTypes() {
        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_8)) {
            final String fileName = "java9/Java9PrivateInterfaceMethod.java"
            instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_1_9)

            // check private methods in interfaces are instrumented
            assertFileMatches(fileName, R_INC + "int i = 0;", false)
        }
    }

    void testModuleInfo() {
        // copy sub-packages - we're not interested in instrumenting them
        FileUtils.dirCopy(srcDir, mGenSrcDir, true);
        // instrument just module-info.java
        File moduleInfo = new File(srcDir, "module-info.java");
        instrumentSourceFile(moduleInfo, JavaEnvUtils.JAVA_1_9)

        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_9)) {
            // compile all stuff
            compileSources(mGenSrcDir, [ "module-info.java"] as String[], JavaEnvUtils.JAVA_1_9)
            // expect no instrumentation in module-info.java
            assertFileMatches("module-info.java", R_INC, true)
        }
    }

    void testDoesNotFailOnModuleInfoKeywordsInRegularSourceFile() {
        File sourceFile = new File(new File(srcDir, "java9"), "NonModuleInfo.java");
        instrumentSourceFile(sourceFile, JavaEnvUtils.JAVA_1_6)
    }

}
