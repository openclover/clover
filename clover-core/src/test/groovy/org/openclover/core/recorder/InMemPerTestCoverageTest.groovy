package org.openclover.core.recorder

import org.openclover.core.CoverageDataSpec
import org.openclover.core.CoverageDataTestBase
import org.openclover.core.registry.Clover2Registry

class InMemPerTestCoverageTest extends CoverageDataTestBase {
    protected CoverageDataSpec newCoverageDataSpec() {
        return new CoverageDataSpec(null, 0, false, true, false, true, PerTestCoverageStrategy.IN_MEMORY)
    }

    protected InMemPerTestCoverage newPerTestCoverage(Clover2Registry registry) {
        return new InMemPerTestCoverage(registry)
    }
}
