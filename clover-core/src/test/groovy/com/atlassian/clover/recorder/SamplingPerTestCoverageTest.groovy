package com.atlassian.clover.recorder

import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.CoverageDataTestBase
import com.atlassian.clover.registry.Clover2Registry

class SamplingPerTestCoverageTest extends CoverageDataTestBase {
    protected CoverageDataSpec newCoverageDataSpec() {
        return new CoverageDataSpec(null, 0, false, true, false, true, PerTestCoverageStrategy.SAMPLING)
    }

    protected PerTestCoverage newPerTestCoverage(Clover2Registry registry) {
        return new SamplingPerTestCoverage(registry, newCoverageDataSpec(), 1000)
    }
}
