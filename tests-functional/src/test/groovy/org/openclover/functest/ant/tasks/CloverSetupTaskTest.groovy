package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic
import org.junit.Test
import org.openclover.ant.tasks.CloverSetupTask

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

@CompileStatic
class CloverSetupTaskTest extends CloverSetupTaskTestBase {

    CloverSetupTaskTest() {
        super("clover-setup.xml")
    }

    @Test
    void testEnable() throws Exception {
        testBase.setUp()
        assertFalse(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
        testBase.expectLogContaining("enable", "OpenClover is enabled with initstring")
        assertTrue(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
    }

    @Test
    void testEnableDisable() throws Exception {
        testBase.setUp()
        assertFalse(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
        testBase.expectLogContaining("enable", "OpenClover is enabled with initstring")
        assertTrue(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
        testBase.expectLogContaining("disable", "OpenClover is disabled")
        assertFalse(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
    }

    @Test
    void testDisableEnable() throws Exception {
        testBase.setUp()
        assertFalse(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
        testBase.expectLogContaining("disable", "OpenClover is disabled")
        assertFalse(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
        testBase.expectLogContaining("enable", "OpenClover is enabled with initstring")
        assertTrue(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))
    }

    @Test
    void testInstrumentLambda() throws Exception {
        testBase.setUp()
        testBase.executeTarget("instrument-lambda")
        testBase.assertLogContains("instrument-lambda all options ok")
    }
}