package org.openclover.idea.coverageview.table;

import org.openclover.core.api.registry.HasMetrics;

public class ComplexityColumnInfo extends AbstractHasMetricsColumnInfo<Integer> {
    private static final String COLUMN_NAME = "Cplx";
    private static final String COLUMN_SIZER = COLUMN_NAME + "  ";

    public ComplexityColumnInfo() {
        super(COLUMN_NAME, RALIGN_CELL_RENDERER);
    }

    @Override
    public String getPreferredStringValue() {
        return COLUMN_SIZER;
    }


    @Override
    protected Integer getValue(HasMetrics hasMetrics) {
        return hasMetrics.getMetrics().getComplexity();
    }

}