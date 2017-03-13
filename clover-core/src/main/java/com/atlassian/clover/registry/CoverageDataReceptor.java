package com.atlassian.clover.registry;

public interface CoverageDataReceptor extends CoverageDataRange {
    void setDataProvider(CoverageDataProvider data);
    CoverageDataProvider getDataProvider();
}
