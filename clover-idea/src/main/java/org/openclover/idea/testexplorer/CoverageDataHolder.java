package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.api.registry.HasMetrics;

public class CoverageDataHolder implements HasCoverageInfo {
    private final HasMetrics element;
    private float coverage;
    private float uniqueCoverage;

    public CoverageDataHolder(HasMetrics element) {
        this.element = element;
    }

    public CoverageDataHolder(HasMetrics element, float coverage, float uniqueCoverage) {
        this.element = element;
        this.coverage = coverage;
        this.uniqueCoverage = uniqueCoverage;
    }

    @Override
    public float getCoverage() {
        return coverage;
    }

    public HasMetrics getElement() {
        return element;
    }

    @Override
    public float getUniqueCoverage() {
        return uniqueCoverage;
    }

    public void setCoverage(float coverage) {
        this.coverage = coverage;
    }

    public void setUniqueCoverage(float uniqueCoverage) {
        this.uniqueCoverage = uniqueCoverage;
    }
}
