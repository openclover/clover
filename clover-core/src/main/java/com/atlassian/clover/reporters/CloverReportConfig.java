package com.atlassian.clover.reporters;

import com.atlassian.clover.CloverDatabase;
import org.openclover.runtime.CloverNames;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.CoverageDataSpec;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.cfg.StorageSize;
import com.atlassian.clover.recorder.PerTestCoverageStrategy;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.util.Path;
import org_openclover_runtime.Clover;

import java.io.File;
import java.util.Date;
import java.util.Map;

import static org.openclover.util.Maps.newHashMap;

public abstract class CloverReportConfig {
    private static final String ERR_NO_OUTFILE_SPECIFIED = "No outfile specified.";

    private CloverReportConfig firstCurrentConfig = null;

    private Format format;
    private Columns columns;

    private String title;
    private String homepage;
    private String projectName;
    private String titleAnchor;
    private String titleTarget;
    private String uniqueTitle; // unique across reports in same <clover-report/> element.
    private File outFile;
    private String mainFileName;
    private boolean needsNewFrame;
    private String initString;
    private Date effectiveDate;
    private Interval span = Interval.DEFAULT_SPAN;
    private StorageSize coverageCacheSize = CoverageData.DEFAULT_EST_PER_TEST_COV_SIZE;
    private String invalidReason;
    private boolean alwaysReport = false;
    private boolean compress = false;
    private Path sourcepath;
    private int titleCount = 1; // the number of reports with this same title
    private boolean loadTestResults = true;
    private boolean loadPerTestData = true;
    private boolean skipCoverageTreeMap =
            System.getProperty(CloverNames.PROP_SKIP_TREE_MAP_REPORT) != null
                    && !"false".equalsIgnoreCase(System.getProperty(CloverNames.PROP_SKIP_TREE_MAP_REPORT));
    private long reportDelay = 3000; // how long to wait if coverage may still be recording in a separate VM

    // A map of reports to link to from this one (title, config)
    private Map<String, CloverReportConfig> linkedReports = newHashMap();

    /**
     * Optional filter instance for determining what to include in the report
     */
    private HasMetricsFilter includeFilter;


    /**
     * Optional filter instance for determining what is considered a "test class" in the report
     */
    private HasMetricsFilter.Invertable testFilter;

    /**
     * The character encoding to use.
     * This will be used in the &lt;meta http-equiv="Content-Type" content="text/html; charset=${charset}"/&gt; tag in
     * the reports.
     */
    private String charset = "UTF-8";

    public void setFirstCurrentConfig(CloverReportConfig currentRoot) {
        this.firstCurrentConfig = currentRoot;
    }

    /**
     * @return the Config to the first current report if this is a linked report. null if not linked or no current
     * reports in the list
     */
    public CloverReportConfig getFirstCurrentConfig() {
        return firstCurrentConfig;
    }

    /**
     * Gets the homepage to use in the main frameset
     *
     * @return the homepage to use
     */
    public String getHomepage() {
        return homepage;
    }

    /**
     * Sets the homepage to be used
     *
     * @param homepage the homepage to use
     */
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    /**
     * Sets the project name to use in the reports
     *
     * @param projectName the name of the project
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Sets the linked reports map.
     */
    public void setLinkedReports(Map<String, CloverReportConfig> links) {
        this.linkedReports = links;
    }

    public Map<String, CloverReportConfig> getLinkedReports() {
        return linkedReports;
    }

    public String getUniqueTitle() {
        return uniqueTitle;
    }

    public void setUniqueTitle(String uniqueTitle) {
        this.uniqueTitle = uniqueTitle;
    }

    public int getTitleCount() {
        return titleCount;
    }

    public int incTitleCount() {
        return titleCount++;
    }

    public void setInitString(String initString) {
        this.initString = initString;
    }

    public String getInitString() {
        return initString;
    }

    public void setOutFile(File outFile) {
        this.outFile = outFile;
        if (outFile != null && outFile.isFile()) {
            setMainFileName(outFile.getName());
        }
    }

    public File getOutFile() {
        return outFile;
    }

    /**
     * This method returns the name of the main file used in this report. e.g. index.html in a Current HTML report.
     *
     * @return the name of the main out file of this report
     */
    public String getMainFileName() {
        return mainFileName;
    }

    /**
     * Gets the main out file for this report.
     * If outFile is a file, it will be returned, otherwise, the main file will be calculated using outFile and
     * mainFileName.
     *
     * @return the main file used to reference this report by
     */
    public File getMainOutFile() {
        return outFile.isFile() ? outFile : new File(outFile, mainFileName);
    }

    public boolean isNeedsNewFrame() {
        return needsNewFrame;
    }

    public void setNeedsNewFrame(boolean needsNewFrame) {
        this.needsNewFrame = needsNewFrame;
    }

    /**
     * The mainFileName is used by HTML reports when rendering links to other reports. This name should be the file part
     * of the full name, as returned by calling {@link File#getName}.
     */
    public void setMainFileName(String mainFileName) {
        this.mainFileName = mainFileName;
    }

    public void setAlwaysReport(boolean alwaysReport) {
        this.alwaysReport = alwaysReport;
    }

    public boolean isAlwaysReport() {
        return alwaysReport;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public void setSourcepath(Path sourcepath) {
        this.sourcepath = sourcepath;
    }

    public Path getSourcepath() {
        return sourcepath;
    }

    /**
     * Set the report title
     *
     * @param title the report's title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleAnchor() {
        return titleAnchor;
    }

    public void setTitleAnchor(String titleAnchor) {
        this.titleAnchor = titleAnchor;
    }

    public String getTitleTarget() {
        return titleTarget;
    }

    public void setTitleTarget(String titleTarget) {
        this.titleTarget = titleTarget;
    }

    public void setSpan(Interval span) {
        this.span = span;
    }

    public Interval getSpan() {
        return span;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setCharset(String encoding) {
        this.charset = encoding;
    }

    public String getCharset() {
        return charset;
    }

    public String getTitle() {
        return title;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    public boolean isColumnsSet() {
        return columns != null;
    }

    public Columns getColumns() {
        return columns != null ? columns : format.getDefaultColumns();
    }

    public void setColumns(Columns columns) {
        this.columns = columns;
    }

    public boolean isLoadTestResults() {
        return loadTestResults;
    }

    public void setLoadTestResults(boolean loadTestResults) {
        this.loadTestResults = loadTestResults;
    }

    public boolean validate() {
        if (getOutFile() == null) {
            setFailureReason(ERR_NO_OUTFILE_SPECIFIED);
            return false;
        }

        return true;
    }

    public String getValidationFailureReason() {
        return invalidReason;
    }

    protected void setFailureReason(String reason) {
        invalidReason = reason;
    }

    public CloverDatabase getCoverageDatabase() throws CloverException {
        CloverDatabase database = null;
        if (getInitString() != null) {
            delayIfRecordingInProgress();
            database = new CloverDatabase(getInitString(), includeFilter, projectName, getFormat().getFilter());

            if (getSourcepath() != null) {
                database.resolve(getSourcepath());
            }
            CoverageDataSpec spec = new CoverageDataSpec(effectiveTestFilter(), getSpan().getValueInMillis());
            spec.setFilterTraces(getFormat().isFilterTrace());
            spec.setLoadPerTestData(isLoadPerTestData());
            spec.setPerTestStrategy(
                    coverageCacheSize.equals(StorageSize.MAX)
                            ? PerTestCoverageStrategy.IN_MEMORY
                            : PerTestCoverageStrategy.SAMPLING);
            spec.setPerTestStorageSize(coverageCacheSize);
            database.loadCoverageData(spec);
        }
        return database;
    }

    protected HasMetricsFilter.Invertable effectiveTestFilter() {
        return testFilter;
    }

    public void setIncludeFilter(HasMetricsFilter includeFilter) {
        this.includeFilter = includeFilter;
    }

    public void setTestFilter(HasMetricsFilter.Invertable testFilter) {
        this.testFilter = testFilter;
    }

    public HasMetricsFilter.Invertable getTestFilter() {
        return testFilter;
    }

    public boolean isLoadPerTestData() {
        return loadPerTestData;
    }

    public void setLoadPerTestData(boolean loadPerTestData) {
        this.loadPerTestData = loadPerTestData;
    }

    public void setCoverageCacheSize(StorageSize size) {
        this.coverageCacheSize = size;
    }

    public long getReportDelay() {
        return reportDelay;
    }

    public void setReportDelay(long delay) {
        this.reportDelay = delay;
    }

    public boolean isSkipCoverageTreeMap() {
        return skipCoverageTreeMap;
    }

    public void setSkipCoverageTreeMap(boolean skipCoverageTreeMap) {
        this.skipCoverageTreeMap = skipCoverageTreeMap;
    }

    private static final int DELAY_INC_MILLIS = 500;

    private void delayIfRecordingInProgress() {
        //If unit tests aren't forked, a final flush may be needed to capture all coverage
        Clover.allRecordersFlush();

        //If out-of-VM coverage recording has happened and appears it may still be happening, wait
        final File liveRecFile = new File(getInitString() + CloverNames.LIVEREC_SUFFIX);
        if (liveRecFile.exists() && !Clover.hasRecorded()) {
            try {
                final long reportDelay = getReportDelay();
                Logger.getInstance().verbose(
                        "Clover has detected that coverage recording may still be in progress. Delaying report generation by up to " + (int) (reportDelay / 1000) + " seconds.");
                for (int i = 0; i < reportDelay && liveRecFile.exists(); i += DELAY_INC_MILLIS) {
                    Thread.sleep(DELAY_INC_MILLIS);
                }
            } catch (InterruptedException e) {
                //Ignore
            }
        }

        if (liveRecFile.exists() && !liveRecFile.delete()) {
            Logger.getInstance().info(
                    "Clover was unable to delete the file " + liveRecFile.getAbsolutePath() + " used to determine if coverage recording is in progress. " +
                            "To speed up future report generation you may wish to delete this file manually.");
        }
    }

}

