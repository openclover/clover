package com.atlassian.clover.util;

import com.atlassian.clover.registry.CoverageDataRange;

public class SimpleCoverageRange implements CoverageDataRange {
    private int dataIndex;
    private int dataLength;

    public SimpleCoverageRange(int dataIndex, int dataLength) {
        this.dataIndex = dataIndex;
        this.dataLength = dataLength;
    }

    @Override
    public int getDataIndex() {
        return dataIndex;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }
}
