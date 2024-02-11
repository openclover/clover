package org.openclover.core.reporters;

/**
 * A Column used to display coverage data.
 * This can be formatted either as raw or a percentage.
 */
public abstract class CoverageColumn extends Column {

    public CoverageColumn(Column col) {
        super(col);
        formatter = col.formatter; // set the formatter
    }

    protected CoverageColumn() {
        formatter = new ColumnFormat.PercentageColumnFormat(); // set the default format
    }

    protected void setValues(int total, float pcValue) {
       setValues(total, pcValue, pcValue < 0);
    }
    protected void setValues(int total, float pcValue, boolean isEmpty) {
       data = new PcColumnData(total, pcValue, isEmpty);
    }

}
