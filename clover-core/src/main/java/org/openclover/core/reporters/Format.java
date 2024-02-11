package org.openclover.core.reporters;

import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.runtime.Logger;

import java.util.Locale;

public class Format {
    public static final String DEFAULT_COMPARATOR_NAME = HasMetricsSupport.CMP_DEFAULT_NAME;

    public static final Format DEFAULT_PDF =
        new Format(Type.PDF, false, DEFAULT_COMPARATOR_NAME, true, false, false, true, "A4");
    public static final Format DEFAULT_XML =
        new Format(Type.XML, false, DEFAULT_COMPARATOR_NAME, true, false, false, true, null);
    public static final Format DEFAULT_HTML =
        new Format(Type.HTML, false, DEFAULT_COMPARATOR_NAME, true, false, true, false, null);
    public static final Format DEFAULT_JSON =
        new Format(Type.JSON, false, DEFAULT_COMPARATOR_NAME, true, false, true, false, null);
    public static final Format DEFAULT_TEXT =
            new Format(Type.TEXT, false, DEFAULT_COMPARATOR_NAME, true, false, false, true, null);

    private static final Columns DEFAULT_HTML_COLUMNS = new Columns();
    static {
        DEFAULT_HTML_COLUMNS.addConfiguredTotalChildren(new Columns.TotalChildren());
        DEFAULT_HTML_COLUMNS.addConfiguredAvgMethodComplexity(new Columns.AvgMethodComplexity());
        Columns.TotalPercentageCovered column = new Columns.TotalPercentageCovered();
        column.setFormat(ColumnFormat.LONGBAR);
        DEFAULT_HTML_COLUMNS.addConfiguredTotalPercentageCovered(column);
    }

    public static final Columns DEFAULT_JSON_COLUMNS = new Columns();
    static {
        DEFAULT_JSON_COLUMNS.addConfiguredTotalPercentageCovered(new Columns.TotalPercentageCovered());
        DEFAULT_JSON_COLUMNS.addConfiguredComplexity(new Columns.Complexity());
        final Columns.CoveredElements coveredElements = new Columns.CoveredElements();
        coveredElements.setFormat("raw");
        DEFAULT_JSON_COLUMNS.addConfiguredCoveredElements(coveredElements);
        final Columns.UncoveredElements uncoveredElements = new Columns.UncoveredElements();
        uncoveredElements.setFormat("raw");
        DEFAULT_JSON_COLUMNS.addConfiguredUncoveredElements(uncoveredElements);
        DEFAULT_JSON_COLUMNS.addConfiguredAvgMethodComplexity(new Columns.AvgMethodComplexity());
    }    

    public static final int MIN_TABWIDTH = 0;
    public static final int MAX_TABWIDTH = 10;

    private Type type;
    private boolean bw = false;
    private String orderby = DEFAULT_COMPARATOR_NAME; //##TODO - this should be an enumerated type
    private boolean showBars = true;
    private boolean noCache = false;
    private boolean srcLevel = true;
    private boolean filterTrace = true;
    private boolean showEmpty = false;
    private String pageSize = "A4"; //##TODO - this should be an enumerated type
    private int tabWidth = 4;
    private int maxNameLength = -1; // -1 indicates no limit
    private String spaceChar = " ";
    private String contextFilter = ""; // a comma or space separated String of context filters to use
    private String callback = "processClover";

    public Format() {
        // no args ctor important for introspecting clients
    }


    public Format(Format that)
    {
        this.type = that.type;
        this.bw = that.bw;
        this.orderby = that.orderby;
        this.showBars = that.showBars;
        this.noCache = that.noCache;
        this.srcLevel = that.srcLevel;
        this.filterTrace = that.filterTrace;
        this.showEmpty = that.showEmpty;
        this.pageSize = that.pageSize;
        this.tabWidth = that.tabWidth;
        this.maxNameLength = that.maxNameLength;
    }


    Format(
        Type type, boolean bw, String orderby, boolean showBars,
        boolean noCache, boolean srcLevel, boolean showEmpty, String pageSize)
    {
        this.type = type;
        this.bw = bw;
        this.orderby = orderby;
        this.showBars = showBars;
        this.noCache = noCache;
        this.srcLevel = srcLevel;
        this.showEmpty = showEmpty;
        this.pageSize = pageSize;
    }


    public void setType(String type) {
        this.type = Type.valueOf(type.toUpperCase(Locale.ENGLISH));
    }

    public void setReportStyle(String reportStyle) {
        //TODO remove CLOV-1795
        Logger.getInstance().warn("Since Clover 4.1.0 the report style attribute is deprecated and will be removed in a future major release.");
    }

    public void setBw(boolean bw) {
        this.bw = bw;
    }

    public void setOrderby(String orderby) {
        this.orderby = orderby;
    }

    public void setShowBars(boolean showBars) {
        this.showBars = showBars;
    }

    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public void setSrcLevel(boolean srcLevel) {
        this.srcLevel = srcLevel;
    }

    public boolean isFilterTrace() {
        return filterTrace;
    }

    public void setFilterTrace(boolean filterTrace) {
        this.filterTrace = filterTrace;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
    }

    public void setPageSize(String pageSize)
    {
        this.pageSize = pageSize.toUpperCase(Locale.ENGLISH);
    }

    public String getPageSize()
    {
        return pageSize;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public void setTabWidth(int tabWidth) {
        if (tabWidth < MIN_TABWIDTH || tabWidth > MAX_TABWIDTH) {
            Logger.getInstance().warn("Ignoring illegal tab width. Outside accepted range " + MIN_TABWIDTH + ".." + MAX_TABWIDTH);
            return;
        }
        this.tabWidth = tabWidth;
    }

    public boolean getShowEmpty() {
        return showEmpty;
    }

    public Type getType() {
        return type;
    }

    public boolean getBw() {
        return bw;
    }

    public String getOrderby() {
        return orderby;
    }

    public boolean getShowBars() {
        return showBars;
    }

    public boolean getNoCache() {
        return noCache;
    }

    public boolean getSrcLevel() {
        return srcLevel;
    }

    public int getMaxNameLength() {
        return maxNameLength;
    }

    public void setMaxNameLength(int maxNameLength) {
        this.maxNameLength = maxNameLength;
    }

    public String getSpaceChar() {
        return spaceChar;
    }

    public void setSpaceChar(String spaceChar) {
        this.spaceChar = spaceChar;
    }

    public String getFilter() {
        return contextFilter;
    }

    public void setFilter(String filter) {
        this.contextFilter = filter;
    }

    public Columns getDefaultColumns() {
        return Type.JSON == type ? DEFAULT_JSON_COLUMNS : DEFAULT_HTML_COLUMNS;
    }

    public String getCallback() {
        return callback;
    }

    /**
     * Allow the empty string "", to disable the callback.
     * @param callback the callback to use for JSON reports
     */
    public void setCallback(String callback) {
        this.callback = (callback == null || callback.length() == 0) ? null : callback;
    }

    public boolean in(Type... types) {
        for (Type type : types) {
            if (this.type == type) {
                return true;
            }
        }
        return false;
    }

}
