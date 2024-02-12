package org.openclover.core;

import org.openclover.core.cfg.StorageSize;
import org.openclover.core.recorder.PerTestCoverageStrategy;
import org.openclover.core.registry.metrics.HasMetricsFilter;

public class CoverageDataSpec {
    private long span = 0;
    private boolean preserveTestCaseCache = false;
    private HasMetricsFilter.Invertable testFilter;
    private boolean resolve = true;
    private boolean filterTraces = true;
    private boolean loadPerTestData = true;
    private boolean deleteUnusedCoverage = false;
    private PerTestCoverageStrategy perTestStrategy = PerTestCoverageStrategy.IN_MEMORY;
    private StorageSize perTestStorageSize = CoverageData.DEFAULT_EST_PER_TEST_COV_SIZE;

    public CoverageDataSpec() {} 

    public CoverageDataSpec(long span) {
        this.span = span;
    }

    public CoverageDataSpec(HasMetricsFilter.Invertable testFilter, long span) {
        this.testFilter = testFilter;
        this.span = span;
    }

    public CoverageDataSpec(
        HasMetricsFilter.Invertable testFilter, long span,
        boolean deleteUnusedCoverage,
        boolean resolve, boolean preserveTestCaseCache,
        boolean loadPerTestData,
        PerTestCoverageStrategy perTestStrategy) {
        this.testFilter = testFilter;
        this.span = span;
        this.resolve = resolve;
        this.preserveTestCaseCache = preserveTestCaseCache;
        this.deleteUnusedCoverage = deleteUnusedCoverage;
        this.loadPerTestData = loadPerTestData;
        this.perTestStrategy = perTestStrategy;
    }

    public boolean isFilterTraces() {
        return filterTraces;
    }

    public void setFilterTraces(boolean filterTraces) {
        this.filterTraces = filterTraces;
    }

    public HasMetricsFilter.Invertable getTestFilter() {
        return testFilter;
    }

    public void setTestFilter(HasMetricsFilter.Invertable testFilter) {
        this.testFilter = testFilter;
    }

    public long getSpan() {
        return span;
    }

    public boolean isResolve() {
        return resolve;
    }

    public boolean isDeleteUnusedCoverage() {
        return deleteUnusedCoverage;
    }

    public boolean isPreserveTestCaseCache() {
        return preserveTestCaseCache;
    }

    public boolean isLoadPerTestData() {
        return loadPerTestData;
    }

    public void setLoadPerTestData(boolean loadPerTestData) {
        this.loadPerTestData = loadPerTestData;
    }

    public PerTestCoverageStrategy getPerTestStrategy() {
        return perTestStrategy;
    }

    public void setPerTestStrategy(PerTestCoverageStrategy perTestStrategy) {
        this.perTestStrategy = perTestStrategy;
    }

    public void setPerTestStorageSize(StorageSize size) {
        this.perTestStorageSize = size;
    }

    public StorageSize getPerTestStorageSize() {
        return perTestStorageSize;
    }
}