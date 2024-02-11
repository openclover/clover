package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic
import org.apache.tools.ant.BuildException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.hamcrest.CoreMatchers.containsString

@CompileStatic
class CloverSetupTaskProfilesInvalidTest extends CloverSetupTaskTestBase {

    @Rule
    public ExpectedException exception = ExpectedException.none()

    CloverSetupTaskProfilesInvalidTest() {
        super("clover-setup-profiles-invalid.xml")
    }

    @Test
    void emptyProfiles() throws Exception {
        exception.expect(BuildException.class)
        exception.expectMessage(containsString("You have defined <profiles> but there is no default <profile> defined"))
        testBase.setUp()
        testBase.executeTarget("invalid-empty-profiles")
    }

    @Test
    void testNoDefaultProfile() throws Exception {
        exception.expect(BuildException.class)
        exception.expectMessage(containsString("You have defined <profiles> but there is no default <profile> defined"))
        testBase.setUp()
        testBase.executeTarget("invalid-no-default-profile")
    }

    @Test
    void testEnable() throws Exception {
        exception.expect(BuildException.class)
        exception.expectMessage(containsString("Invalid value of the coverageRecorder attribute"))
        testBase.setUp()
        testBase.executeTarget("invalid-coverage-recorder")
    }
}