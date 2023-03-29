package com.atlassian.clover.ant.tasks

class CloverEnvTaskTest extends CloverBuildFileTestBase {
    CloverEnvTaskTest(String name) {
        super(name)
    }

    String getAntFileName() {
        return "clover-env.xml"
    }

    void testCloverEnvImport() {
        executeTarget("testCloverEnvImport")
        assertNotNull(getProject().getTargets().get("with.clover"))
        assertNotNull(getProject().getTargets().get("clover.report"))
        assertNotNull(getProject().getTargets().get("clover.current"))
        assertNotNull(getProject().getTargets().get("clover.save-history"))
        assertNotNull(getProject().getTargets().get("clover.all"))
        assertNotNull(getProject().getTargets().get("clover.clean"))
    }
}
