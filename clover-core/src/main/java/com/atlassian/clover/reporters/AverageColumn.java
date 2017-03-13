package com.atlassian.clover.reporters;

import com.atlassian.clover.Logger;

/**
 * An average column displays float data.
 */
public abstract class AverageColumn extends TotalColumn {

    public AverageColumn(Column col) {
        super(col);
        setFormat(ColumnFormat.RAW);
    }

    protected AverageColumn() {
        setFormat(ColumnFormat.RAW);
    }

    @Override
    public void setFormat(String format) {
        // only RAW is allowed on totals.
        if (!ColumnFormat.RAW.equalsIgnoreCase(format)) {
            Logger.getInstance().warn("An Average column may only have format='" + ColumnFormat.RAW +
                            "', not format='" + format + "'. Using format: " + ColumnFormat.RAW);
        }
        // always render average as a float.
        formatter = new ColumnFormat.FloatColumnFormat();
    }

    public void setValues(float average) {
        data = new ColumnData(average);
    }
}
