package org.openclover.core.registry;

public interface CoverageDataReceptor extends CoverageDataRange {
    void setDataProvider(CoverageDataProvider data);
    CoverageDataProvider getDataProvider();
}
