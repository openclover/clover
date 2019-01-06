package com.atlassian.clover.reporters;

import com.atlassian.clover.Logger;
import com.atlassian.clover.util.Formatting;
import com.atlassian.clover.reporters.html.HtmlReporter;

import java.util.Map;

import static clover.com.google.common.collect.Maps.newHashMap;

/**
 * A class used to render values and titles of specific columns.
 */
public interface ColumnFormat {

    final static String PERCENTAGE = "%";
    final static String PC = "pc";
    final static String RAW = "raw";
    final static String BAR = "bar";
    final static String LONGBAR = "longbar";
    final static String SHORTBAR = "shortbar";

    public static final String SORT_TYPE_NUMBER = "number";
    public static final String SORT_TYPE_ALPHA = "alpha";

    String format(Column.ColumnData data);

    Number formatNumber(Column.ColumnData data);

    String formatTitle(String name);

    /**
     * Formats the value to sort by.
     *
     * @return the value to sort by.
     */
    String sortValue(Column.ColumnData data);

    /**
     * Returns the type of sorting required for sort value.
     *
     * @return the sort type, one of {@link #SORT_TYPE_ALPHA} or {@link #SORT_TYPE_NUMBER}
     */
    String sortType();

    int getColSpan();

    boolean isWithinThreshold(Column.ColumnData data, float min, float max);

    static class Factory {

        private static final Map<String, ColumnFormat> FORMATS = newHashMap();
        static {
            FORMATS.put(RAW, new FloatColumnFormat());
            FORMATS.put(BAR, new ShortBarGraphColumnFormat());
            FORMATS.put(SHORTBAR, new ShortBarGraphColumnFormat());
            FORMATS.put(LONGBAR, new LongBarGraphColumnFormat());
            FORMATS.put(PC, new PercentageColumnFormat());
            FORMATS.put(PERCENTAGE, new PercentageColumnFormat());
        }

        static ColumnFormat createFormat(String format) {
            return FORMATS.containsKey(format) ? FORMATS.get(format) : new PercentageColumnFormat();
        }
    }

    static class IntColumnFormat extends FloatColumnFormat {
        @Override
        public String format(Column.ColumnData data) {
            return Formatting.formatInt((int) data.getValue());
        }

        @Override
        public Number formatNumber(Column.ColumnData data) {
            return (int) data.getValue();
        }
    }

    /**
     * A formatter to use when there is no data and an error message should be displayed instead.
     */
    static class ErrorColumnFormat implements ColumnFormat {

        private final String errorMsg;

        public ErrorColumnFormat(String msg) {
            errorMsg = msg;
        }

        @Override
        public String format(Column.ColumnData data) {
            return errorMsg;
        }

        @Override
        public Number formatNumber(Column.ColumnData data) {
            return -1;
        }

        @Override
        public String formatTitle(String name) {
            return name;
        }

        @Override
        public String sortValue(Column.ColumnData data) {
            return "-";
        }

        @Override
        public String sortType() {
            return "";
        }

        @Override
        public int getColSpan() {
            return 1;
        }

        @Override
        public boolean isWithinThreshold(Column.ColumnData data, float min, float max) {
            return true;
        }
    }

    static class FloatColumnFormat implements ColumnFormat {

        @Override
        public String format(Column.ColumnData data) {
            return Formatting.format2d(data.getValue());
        }

        @Override
        public Number formatNumber(Column.ColumnData data) {
            return new Float(data.getValue());
        }

        @Override
        public String formatTitle(String name) {
            return name;
        }

        @Override
        public String sortValue(Column.ColumnData data) {
            return Float.toString(data.getValue());
        }

        @Override
        public String sortType() {
            return SORT_TYPE_NUMBER;
        }

        @Override
        public int getColSpan() {
            return 1;
        }

        @Override
        public boolean isWithinThreshold(Column.ColumnData data, float min, float max) {
            return  min <= data.getValue() && data.getValue() <= max;
        }
    }

    static class PercentageColumnFormat extends FloatColumnFormat {

        @Override
        public String format(Column.ColumnData data) {

            return Formatting.getPercentStr(toPcData(data).getPcValue());
        }

        @Override
        public Number formatNumber(Column.ColumnData data) {
            return new Float(toPcData(data).getPcValue() * 100);
        }

        @Override
        public String sortValue(Column.ColumnData data) {
            return Float.toString(toPcData(data).getPcValue());
        }

        @Override
        public String formatTitle(String name) {
            return "% " + name;
        }

        @Override
        public boolean isWithinThreshold(Column.ColumnData data, float min, float max) {
            float pcValue = toPcData(data).getPcValue() * 100;
            return min <= pcValue && pcValue <= max;
        }

        Column.PcColumnData toPcData(Column.ColumnData data) {
            return (Column.PcColumnData) data;
        }

    }

    abstract static class BarGraphColumnFormat extends PercentageColumnFormat {

        @Override
        public String format(Column.ColumnData data) {
            try {
                return HtmlReporter.renderHtmlBarTable(
                        toPcData(data).getPcValue(),
                        getBarSize(),
                        data.getCustomClass(),
                        toPcData(data).getCustomPositiveClass(),
                        toPcData(data).getCustomNegativeClass());
            } catch (Exception e) {
                Logger.getInstance().warn("Error rendering bar graph.", e);
            }
            return "-";
        }

        @Override
        public String formatTitle(String name) {
            return name;
        }

        @Override
        public int getColSpan() {
            return 2;
        }

        abstract int getBarSize();
    }

    static class ShortBarGraphColumnFormat extends BarGraphColumnFormat {
        @Override
        int getBarSize() {
            return 40;
        }
    }

    static class LongBarGraphColumnFormat extends BarGraphColumnFormat {
        @Override
        int getBarSize() {
            return 200;
        }
    }
}
