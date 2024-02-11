package org.openclover.core.reporters;

import org.openclover.runtime.Logger;


/**
 * A TotalColumn is a column used to display global data,
 * from the entire project, not taking into account if it were
 * covered or not.
 */
public abstract class TotalColumn extends Column {

    public TotalColumn() {
        formatter = new ColumnFormat.FloatColumnFormat();// the default format
    }

    public TotalColumn(Column col) {
        super(col);
        formatter = new ColumnFormat.FloatColumnFormat();// the default format        
    }

    @Override
    public void setFormat(String format) {
        // only RAW is allowed on totals.
        if (!ColumnFormat.RAW.equalsIgnoreCase(format)) {
                Logger.getInstance().warn(
                    "A Total column may only have format='" + ColumnFormat.RAW +
                            "', not format='" + format + "'. Using format: " + ColumnFormat.RAW );
            format = ColumnFormat.RAW;
        }
        super.setFormat(format);
    }

    public void setValues(int total) {
        data = new ColumnData(total);
    }
}
