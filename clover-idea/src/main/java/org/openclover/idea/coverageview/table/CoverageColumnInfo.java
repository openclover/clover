package org.openclover.idea.coverageview.table;

import org.openclover.idea.coverage.PercentBarColors;
import org.openclover.idea.testexplorer.PercentBarTableCellRenderer;
import com.atlassian.clover.api.registry.HasMetrics;

public class CoverageColumnInfo extends AbstractHasMetricsColumnInfo<Float> {
    private static final String COLUMN_NAME = "Coverage";
    private static final String COLUMN_SIZER = "XXXXXXXXXXXXX";

    public CoverageColumnInfo() {
        super(COLUMN_NAME, PercentBarTableCellRenderer.getInstance(PercentBarColors.GREEN_ON_RED));
    }

    @Override
    public String getPreferredStringValue() {
        return COLUMN_SIZER;
    }


    @Override
    protected Float getValue(HasMetrics hasMetrics) {
        return hasMetrics.getMetrics().getPcCoveredElements();
    }

}