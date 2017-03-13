package com.atlassian.clover.reporters

import org.junit.Test

import static org.junit.Assert.assertFalse

/**
 * Test for Current class.
 */
class CurrentTest {
    @Test
    void testShowUniqueCoverageDefaultsToFalse() throws Exception {
        Current current = new Current()
        assertFalse(current.isShowUniqueCoverage())
    }
}
