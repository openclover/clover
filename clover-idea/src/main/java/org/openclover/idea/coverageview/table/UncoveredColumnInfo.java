package org.openclover.idea.coverageview.table;

import com.atlassian.clover.api.registry.HasMetrics;

public class UncoveredColumnInfo extends AbstractHasMetricsColumnInfo<Integer> {
    private static final String COLUMN_NAME = "#Uncovered";
    private static final String COLUMN_SIZER = COLUMN_NAME + "  ";

    public UncoveredColumnInfo() {
        super(COLUMN_NAME, RALIGN_CELL_RENDERER);
    }

    @Override
    public String getPreferredStringValue() {
        return COLUMN_SIZER;
    }


    @Override
    protected Integer getValue(HasMetrics hasMetrics) {
        return hasMetrics.getMetrics().getNumUncoveredElements();
    }

}