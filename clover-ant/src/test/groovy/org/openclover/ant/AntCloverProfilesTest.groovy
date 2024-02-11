package org.openclover.ant

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.assertEquals

/**
 * Test for {@link org.openclover.ant.AntCloverProfiles}
 */
class AntCloverProfilesTest {

    @Rule
    public ExpectedException exception = ExpectedException.none()

    /**
     * Simple setter test.
     */
    @Test
    void testAddConfiguredProfileSingleDefaultProfile() {
        AntCloverProfiles profiles = new AntCloverProfiles()
        profiles.addConfiguredProfile(new AntCloverProfile())
        assertEquals(1, profiles.getProfiles().size())
    }

    /**
     * Every profile must have unique name.
     * @throws Exception
     */
    @Test
    void testAddConfiguredProfileDuplicateName() {
        exception.expect(IllegalArgumentException.class)
        exception.expectMessage(containsString("Duplicated value in the <profile name"))
        AntCloverProfiles profiles = new AntCloverProfiles()
        profiles.addConfiguredProfile(new AntCloverProfile())
        profiles.addConfiguredProfile(new AntCloverProfile())
    }
}
