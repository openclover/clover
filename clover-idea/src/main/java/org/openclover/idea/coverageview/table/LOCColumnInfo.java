package org.openclover.idea.coverageview.table;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.registry.metrics.FileMetrics;

import java.text.DecimalFormat;

public class LOCColumnInfo extends AbstractHasMetricsColumnInfo<String> {
    private static final String COLUMN_NAME = "Lines";
    private static final String COLUMN_SIZER = "9 999 999";

    private static final DecimalFormat FORMAT = new DecimalFormat("# ###");

    public LOCColumnInfo() {
        super(COLUMN_NAME, RALIGN_CELL_RENDERER);
    }

    @Override
    public String getPreferredStringValue() {
        return COLUMN_SIZER;
    }


    @Override
    protected String getValue(HasMetrics hasMetrics) {
        final int range;
        if (hasMetrics instanceof SourceInfo) {
            final SourceInfo srcRegion = (SourceInfo) hasMetrics;
            range = srcRegion.getEndLine() - srcRegion.getStartLine() + 1;
        } else {
            final BlockMetrics metrics = hasMetrics.getMetrics();
            if (metrics instanceof FileMetrics) {
                range = ((FileMetrics) metrics).getLineCount();
            } else {
                return null;
            }
        }

        return FORMAT.format(range);
    }
}