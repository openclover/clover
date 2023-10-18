package com.atlassian.clover.ant.tasks

import groovy.transform.CompileStatic

@CompileStatic
class CloverRuntimeFailTest extends CloverBuildFileTestBase {
    CloverRuntimeFailTest(String name) {
        super(name)
    }

    void testCloverClassesMissing() {
/*
        executeTarget("testCloverClassesMissing")
        //Occurs in the static initializer of the recorder class
        assertAntOutputContains("[CLOVER] FATAL ERROR: Clover could not be initialised. Are you sure you have Clover in the runtime classpath? (class java.lang.NoClassDefFoundError:com_atlassian_clover/CloverVersionInfo)")
        //Occurs on the first call to Main$__CLR2_5_0XYZ.R.inc(0) in instrumented code
        assertAntOutputContains("Exception in thread \"main\" java.lang.NoClassDefFoundError: com_atlassian_clover/CoverageRecorder")
*/
    }

    void testMissingDb() {
/*
        executeTarget("testMissingDb")
        //Instrumented application continued to run
        assertAntOutputContains("Hello, world")
        //User was warned
        assertAntOutputContains("ERROR: CLOVER: Unable to load the coverage database at")
        assertAntOutputContains("ERROR: CLOVER: No coverage data will be gathered.")
*/
    }

     void testSecurityException() {
/*
        executeTarget("testSecurityException")
        //Instrumented application continued to run
        assertAntOutputContains("Hello, world")
        //User was warned
        assertAntOutputContains(Clover.SECURITY_EXCEPTION_MSG)
*/
    }

      void testSecurityPolicy() throws Exception {
/*
        executeTarget("testSecurityPolicy")
        assertAntOutputContains("Hello, world")
        assertFalse(getOutput().contains("java.security.AccessControlException"))
*/
    }

    String getAntFileName() {
        return "clover-runtimefail.xml"
    }
}