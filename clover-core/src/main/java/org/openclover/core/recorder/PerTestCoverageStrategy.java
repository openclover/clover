package org.openclover.core.recorder;

import org.openclover.core.CoverageDataSpec;
import org.openclover.core.registry.Clover2Registry;

public enum PerTestCoverageStrategy {
    IN_MEMORY() {
        @Override
        public PerTestCoverage build(Clover2Registry registry, CoverageDataSpec spec, int estPerTestRecordings) {
            return new InMemPerTestCoverage(registry);
        }},
    SAMPLING() {
        @Override
        public PerTestCoverage build(Clover2Registry registry, CoverageDataSpec spec, int estPerTestRecordings) {
            return new SamplingPerTestCoverage(registry, spec, estPerTestRecordings);
        }};

    public abstract PerTestCoverage build(Clover2Registry registry, CoverageDataSpec spec, int estPerTestRecordings);
}
