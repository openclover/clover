package org.openclover.core.reporters;

import org.openclover.core.cfg.Interval;
import org.openclover.core.cfg.Percentage;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.openclover.core.util.Lists.newArrayList;


public class Historical extends CloverReportConfig {
    private static final String ERR_TIME_INTERVAL_ENDS_BEFORE_START
        = "Invalid time interval: 'from' time greater than 'to' time.";
    private static final String ERR_RANGE_MUST_BE_POSITIVE_INTEGER
        = "Range must be a positive integer";
    private static final String ERR_HISTORY_DATA_DIR_NOT_SPECIFIED
            = "History data dir not specified.";

    private static final Columns DEFAULT_METRICS_COLUMNS = new Columns();
    private static final Columns DEFAULT_COVERAGE_COLUMNS = new Columns();
    private static final Column DEFAULT_MOVERS_COLUMN = new Columns.TotalPercentageCovered();

    static {
        DEFAULT_METRICS_COLUMNS.addConfiguredTotalPackages(new Columns.TotalPackages());
        DEFAULT_METRICS_COLUMNS.addConfiguredTotalFiles(new Columns.TotalFiles());
        DEFAULT_METRICS_COLUMNS.addConfiguredNcLineCount(new Columns.NcLineCount());
        DEFAULT_METRICS_COLUMNS.addConfiguredLineCount(new Columns.LineCount());
        DEFAULT_METRICS_COLUMNS.addConfiguredComplexity(new Columns.Complexity());

        DEFAULT_COVERAGE_COLUMNS.addConfiguredCoveredElements(new Columns.CoveredElements());
        DEFAULT_COVERAGE_COLUMNS.addConfiguredCoveredMethods(new Columns.CoveredMethods());
        DEFAULT_COVERAGE_COLUMNS.addConfiguredCoveredStatements(new Columns.CoveredStatements());
        DEFAULT_COVERAGE_COLUMNS.addConfiguredCoveredBranches(new Columns.CoveredBranches());
    }

    public static class Overview {
    }

    public static class Chart {
        protected boolean logScale = true;
        protected Columns columns = DEFAULT_COVERAGE_COLUMNS;
        private int height = 480;
        private int width = 640;
        private String xLabel;
        protected String yLabel;
        protected String title;
        protected int upperBound = -1;
        protected boolean autoRange = false;

        public void setInclude(String include) {
            //TODO reword this to make sense from maven and cli if required...
            Logger.getInstance().warn("WARN: The include attribute is deprecated. Use the nested <columns/> element instead.");
        }

        public void addColumns(Columns cols) {
            columns = cols;
        }

        public Columns getColumns() {
            return columns;
        }

        public void setLogScale(boolean logScale) {
            this.logScale = logScale;
        }

        public boolean isLogScale() {
            return logScale;
        }

        public String getTitle() {
            return title;
        }

        public String getYLabel() {
            return yLabel;
        }

        public String getXLabel() {
            return xLabel;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public void setXLabel(String xLabel) {
            this.xLabel = xLabel;
        }

        public void setYLabel(String yLabel) {
            this.yLabel = yLabel;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getUpperBound() {
            return upperBound;
        }

        public void setUpperBound(int upperBound) {
            this.upperBound = upperBound;
        }

        public boolean isAutoRange() {
            return autoRange;
        }

        public void setAutoRange(boolean autoRange) {
            this.autoRange = autoRange;
        }
    }

    public static class Coverage extends Chart {
        public Coverage() {
            columns = DEFAULT_COVERAGE_COLUMNS;
            yLabel = "Coverage (%)";
            title = "Coverage";
            logScale = false;
            upperBound = 100;
        }
    }

    public static class Metrics extends Chart {
        public Metrics() {
            columns = DEFAULT_METRICS_COLUMNS;
            yLabel = "";
            title = "Metrics";
        }
    }

    public static class Added extends Movers {
        protected static Percentage DEFAULT_THRESHOLD = new Percentage("0%");
        public static final Movers DEFAULT_MOVERS
            = new Added(null, DEFAULT_RANGE, DEFAULT_MOVERS_COLUMN);

        public Added() {
        }

        public Added(Interval interval, int range, Column column) {
            super(interval, DEFAULT_THRESHOLD, range, column);
        }
        
        @Override
        public void setThreshold(Percentage threshold) throws CloverException {
            throw new CloverException("'Added' element does not support the threshold attribute");
        }

    }

    public static class Movers {
        protected static Percentage DEFAULT_THRESHOLD = new Percentage("1%");
        protected static int DEFAULT_RANGE = 5;
        public static final Movers DEFAULT_MOVERS
            = new Movers(null, DEFAULT_THRESHOLD, DEFAULT_RANGE, DEFAULT_MOVERS_COLUMN);

        private Interval interval;
        private Percentage threshold =  DEFAULT_THRESHOLD;
        private int range = DEFAULT_RANGE;
        private Column column = DEFAULT_MOVERS_COLUMN;
        private int maxWidth;

        public Movers()
        {
        }

        public Movers(Interval interval, Percentage threshold, int range, Column column)
        {
            this.interval = interval;
            this.threshold = threshold;
            this.range = range;
            this.column = column;
        }

        @SuppressWarnings("unused") // Ant parameter
        public void addConfiguredColumns(Columns cols) throws CloverException {
            Set columns = cols.getProjectColumns();
            if (columns.size() != 1) {
                throw new CloverException("Movers only accepts columns containing 1 column element.");
            }
            this.column = (Column) columns.iterator().next();
        }

        public void setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public void setColumn(Column column) {
            this.column = column;
        }

        public Column getColumn() {
            return column;
        }

        public void setInterval(Interval interval)
        {
            this.interval = interval;
        }

        public Interval getInterval()
        {
            return interval;
        }

        public void setThreshold(Percentage threshold) throws CloverException {
            this.threshold = threshold;
        }

        public Percentage getThreshold()
        {
            return threshold;
        }

        public void setRange(int range)
        {
            this.range = range;
        }

        public int getRange()
        {
            return range;
        }
    }

    private File historyDir;
    protected File[] historyFiles;
    private String from;
    private String to;
    private String dateFormat;
    private String packageName;
    private Date fromTS;
    private Date toTS;
    private List<Chart> charts = newArrayList();
    private List<Movers> allMovers = newArrayList();
    private List<Added> allAdded = newArrayList();
    private Overview overview;
    private boolean json = true; // whether or not to generate JSON historical data

    public static final Date DEFAULT_FROM_TS = new Date(0);
    public static final Date DEFAULT_TO_TS = new Date(Long.MAX_VALUE);

    public void setHistoryDir(File historyDir) {
        this.historyDir = historyDir;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public void addCoverage(Coverage coverage) {
        charts.add(coverage);
    }

    public void addMetrics(Metrics metrics) {
        charts.add(metrics);
    }

    public void addChart(Chart chart) {
        charts.add(chart);
    }

    public void addMovers(Movers movers) {
        allMovers.add(movers);
    }

    public void addAdded(Added movers) {
        allAdded.add(movers);
    }

    public void addOverview(Overview overview) {
        this.overview = overview;
    }

    public Overview getOverview() {
        return overview;
    }

    public List getCharts() {
        return charts;
    }

    public List<Movers> getMovers() {
        return allMovers;
    }

    public List<Added> getAdded() {
        return allAdded;
    }

    public File getHistoryDir() {
        return historyDir;
    }

    public File[] getHistoryFiles() {
        return historyFiles;
    }

    public String getFrom() {
        return from;
    }
    public String getTo() {
        return to;
    }
    public String getDateFormat() {
        return dateFormat;
    }
    public String getPackage() {
        return packageName;
    }

    public Date getFromTS()
    {
        return fromTS;
    }

    public Date getToTS()
    {
        return toTS;
    }

    public boolean isJson() {
        return json;
    }

    public void setJson(boolean json) {
        this.json = json;
    }

    @Override
    public boolean validate()
    {
        if (!super.validate()) {
            return false;
        }

        if (getFormat() == null) {
            setFormat(Format.DEFAULT_HTML);
        } else if (!getFormat().in(Type.HTML, Type.PDF)) {
            setFailureReason("Report format " + getFormat().getType() + " is not supported by historical reports.");
            return false;
        }

        if (getHistoryDir() == null) {
            setFailureReason(ERR_HISTORY_DATA_DIR_NOT_SPECIFIED);
            return false;
        }

        return processAndValidate();
    }

    public boolean processAndValidate()
    {
        processDateRange();

        if (getFromTS().after(getToTS())) {
            setFailureReason(ERR_TIME_INTERVAL_ENDS_BEFORE_START);
            return false;
        }

        if (overview == null &&  charts.size() == 0 && allMovers.size() == 0 && allAdded.size() == 0) {
            // nothing specified to render, so render a default report
            addOverview(new Overview());
            addMetrics(new Metrics());
            addCoverage(new Coverage());
            addMovers(Movers.DEFAULT_MOVERS);
            addAdded((Added)Added.DEFAULT_MOVERS);
        }

        for (Movers movers : allMovers) {
            if (movers != null && movers.getRange() <= 0) {
                setFailureReason(ERR_RANGE_MUST_BE_POSITIVE_INTEGER);
                return false;
            }
        }

        for (Added added : allAdded) {
            if (added != null && added.getRange() <= 0) {
                setFailureReason(ERR_RANGE_MUST_BE_POSITIVE_INTEGER);
                return false;
            }
        }

        return true;
    }

    private void processDateRange()
    {
        SimpleDateFormat df;

        SimpleDateFormat def = new SimpleDateFormat();
        if (dateFormat == null) {
            df = def;
        } else {
            try {
                df = new SimpleDateFormat(dateFormat);
            }
            catch (Throwable e) {
                Logger.getInstance().warn("Invalid dateFormat: " + dateFormat + ". " + e.getMessage());
                df = def;
            }
        }

        fromTS = parseDate(df, from, DEFAULT_FROM_TS);
        toTS = parseDate(df, to, DEFAULT_TO_TS);
    }

    private Date parseDate(SimpleDateFormat df, String dateStr, Date defaultDate) {
        Date date;
        if (dateStr != null) {
            try {
                date = df.parse(dateStr);
            }
            catch (Exception e) {
                // ignore, use default
                Logger.getInstance().warn("Date has invalid format: '" + dateStr + "'. " + e.getMessage() + ". Using default value instead: " + defaultDate);
                date = defaultDate;
            }
        }
        else {
            date = defaultDate;
        }
        return date;
    }
}