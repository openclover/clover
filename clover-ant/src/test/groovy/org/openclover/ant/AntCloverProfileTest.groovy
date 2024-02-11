package org.openclover.ant

import org_openclover_runtime.CloverProfile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.assertEquals

/**
 * Test for {@link AntCloverProfile}
 */
class AntCloverProfileTest {

    @Rule
    public ExpectedException exception = ExpectedException.none()

    @Test
    void testDefaultValues() {
        AntCloverProfile profile = new AntCloverProfile()
        assertEquals(CloverProfile.DEFAULT_NAME, profile.getName())
        assertEquals(CloverProfile.DEFAULT_COVERAGE_RECORDER, profile.getCoverageRecorder().toString())
        assertEquals(null, profile.getDistributedCoverage())
    }

    @Test
    void testCoverageRecorderFixed() {
        AntCloverProfile profile = new AntCloverProfile()
        // recorder name is case insensitive
        profile.setCoverageRecorder("fixed")
        assertEquals(CloverProfile.CoverageRecorderType.FIXED, profile.getCoverageRecorder())
        profile.setCoverageRecorder("FIXED")
        assertEquals(CloverProfile.CoverageRecorderType.FIXED, profile.getCoverageRecorder())
    }

    @Test
    void testCoverageRecorderGrowable() {
        AntCloverProfile profile = new AntCloverProfile()
        profile.setCoverageRecorder("growable")
        assertEquals(CloverProfile.CoverageRecorderType.GROWABLE, profile.getCoverageRecorder())
    }

    @Test
    void testCoverageRecorderShared() {
        AntCloverProfile profile = new AntCloverProfile()
        profile.setCoverageRecorder("shared")
        assertEquals(CloverProfile.CoverageRecorderType.SHARED, profile.getCoverageRecorder())
    }

    @Test
    void testCoverageRecorderInvalid() {
        exception.expect(IllegalArgumentException.class)
        exception.expectMessage(containsString("Invalid value of the coverageRecorder attribute."))
        AntCloverProfile profile = new AntCloverProfile()
        profile.setCoverageRecorder("invalidRecorderName")
    }
}
