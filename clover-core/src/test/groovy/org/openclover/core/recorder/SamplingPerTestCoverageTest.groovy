package org.openclover.core.recorder

import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.recorder.PerTestCoverage
import com.atlassian.clover.recorder.PerTestCoverageStrategy
import com.atlassian.clover.recorder.SamplingPerTestCoverage
import com.atlassian.clover.registry.Clover2Registry
import org.openclover.core.CoverageDataTestBase

class SamplingPerTestCoverageTest extends CoverageDataTestBase {
    protected CoverageDataSpec newCoverageDataSpec() {
        return new CoverageDataSpec(null, 0, false, true, false, true, PerTestCoverageStrategy.SAMPLING)
    }

    protected PerTestCoverage newPerTestCoverage(Clover2Registry registry) {
        return new SamplingPerTestCoverage(registry, newCoverageDataSpec(), 1000)
    }
}
