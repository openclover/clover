package com.atlassian.clover

import org.apache.tools.ant.util.JavaEnvUtils


/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK1.9
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
        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_9)) {
            instrumentAndCompileSourceFile(srcDir, mGenSrcDir, "module-info.java", JavaEnvUtils.JAVA_1_9)

            // expect no Clover instrumentation in module-info.java
            assertFileMatches(fileName, R_INC, true)
        }
    }
}
