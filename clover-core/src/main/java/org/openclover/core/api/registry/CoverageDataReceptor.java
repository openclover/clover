package org.openclover.core.api.registry;

public interface CoverageDataReceptor extends CoverageDataRange {
    CoverageDataProvider getDataProvider();
    void setDataProvider(CoverageDataProvider data);
}
