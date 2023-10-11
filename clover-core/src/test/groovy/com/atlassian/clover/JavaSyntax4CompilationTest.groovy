package com.atlassian.clover

import org.apache.tools.ant.util.JavaEnvUtils

/**
 * The purpose of this test is to
 * <li>make sure the code compiles under JDK1.4 or later</li>
 * <li>make sure that when that code is instrumented, it still compiles</li>
 */
class JavaSyntax4CompilationTest extends JavaSyntaxCompilationTestBase {

    /**
     * Test java 1.4 language features and how clover handles them.
     *
     * @throws Exception
     */
    void testInstrumentationAndCompilation_14() throws Exception {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.4")
        resetAntOutput()
        instrumentAndCompileSources(srcDir, JavaEnvUtils.JAVA_1_7)

        // verify that instrumentation was made
        // input : assert i > 0
        // output: assert (((i > 0)&&(__CLR3_1_600h31bv6gv.R.iget(1)!=0|true))||(__CLR3_1_600h31bv6gv.R.iget(2)==0&false))
        assertFileMatches("Assertion.java", ".*assert.*i \\> 0.*__CLR.*", false)
    }

}
