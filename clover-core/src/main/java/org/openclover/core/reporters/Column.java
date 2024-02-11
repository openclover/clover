package org.openclover.core.reporters;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.runtime.api.CloverException;

/**
 * A holder of column information.
 * <p/>
 * Each column gets its own ColumnFormat that is used to
 * format the data and even title of the column.
 */
public abstract class Column {

    protected ColumnFormat formatter;
    private String format;
    private String scope;

    protected ColumnData data;

    private float min = Float.NEGATIVE_INFINITY;
    private float max = Float.POSITIVE_INFINITY;

    static final String ALERT_STYLE = "thresholdAlert";

    public Column() {
    }

    public Column(Column col) {
        if (col.format != null) {
            setFormat(col.format);
        }
        setMin(col.min);
        setMax(col.max);
        scope = col.scope;
    }

    public abstract Column copy();

    public void setFormat(String format) {
        this.formatter = ColumnFormat.Factory.createFormat(format);
        this.format = format;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public String getStyle() {
        return formatter.isWithinThreshold(data, min, max) ? "" :  ALERT_STYLE;
    }

    public ColumnFormat getFormat() {
        return formatter;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public void setCustomClass(String customClass) {
        this.data.setCustomClass(customClass);
    }

    /**
     * Renders the value in this column.
     *
     * @return the formatted string, as returned by {@link #formatter}
     */
    public String render() {
        // note: this method is used only by PDFReporter (to render bar graph), so should be fine to have ADG by default
        return formatter.format(data);
    }

    public Number getNumber() {
        return formatter.formatNumber(data);
    }

    public ColumnData getColumnData() {
        return data;
    }

    /**
     * Returns the value used to sort this column by.
     *
     * @return the sort value
     */
    public String sortValue() {
        return formatter.sortValue(data);
    }

    public abstract void init(BlockMetrics value) throws CloverException;

    public void reset() {
        data = null;
    }

    public String getName() {
        final String[] className = this.getClass().getName().split("\\$");
        return className[className.length - 1];
    }

    public abstract String getTitle(BlockMetrics value);

    public String getTitle() {
        return getTitle(null);
    }

    public String getHelp() {
        return null; // no help available by default
    }

    public static class ColumnData {
        private float value;
        private String customClass = "";


        protected ColumnData(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }

        public String getCustomClass() {
            return customClass;
        }

        public void setCustomClass(String customClass) {
            this.customClass = customClass;
        }
    }

    public static class PcColumnData extends ColumnData {

        private float pcValue;
        private boolean empty;
        private String customPositiveClass = "";
        private String customNegativeClass = "";
        
        public PcColumnData(int value, boolean empty) {
            super(value);
            this.empty = empty;
        }

        public PcColumnData(int value, float pcValue) {
            super(value);
            this.pcValue = pcValue;
            this.empty = pcValue < 0;
        }
        public PcColumnData(int value, float pcValue, boolean empty) {
            super(value);
            this.pcValue = pcValue;
            this.empty = empty;
        }

        public float getPcValue() {
            return pcValue;
        }

        public boolean isEmpty() {
            return empty;
        }

        public String getCustomPositiveClass() {
            return customPositiveClass;
        }

        public String getCustomNegativeClass() {
            return customNegativeClass;
        }

        public void setCustomPositiveClass(String className) {
            this.customPositiveClass = className;
        }

        public void setCustomNegativeClass(String className) {
            this.customNegativeClass = className;
        }
    }



}
