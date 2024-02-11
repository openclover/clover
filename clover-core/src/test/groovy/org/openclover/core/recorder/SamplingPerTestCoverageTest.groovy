package org.openclover.core.recorder

import org.openclover.core.CoverageDataSpec
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.CoverageDataTestBase

class SamplingPerTestCoverageTest extends CoverageDataTestBase {
    protected CoverageDataSpec newCoverageDataSpec() {
        return new CoverageDataSpec(null, 0, false, true, false, true, PerTestCoverageStrategy.SAMPLING)
    }

    protected PerTestCoverage newPerTestCoverage(Clover2Registry registry) {
        return new SamplingPerTestCoverage(registry, newCoverageDataSpec(), 1000)
    }
}
