package com.atlassian.clover.reporters;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.Logger;
import com.atlassian.clover.MaskedBitSetCoverageProvider;
import com.atlassian.clover.TestResultProcessor;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import com.atlassian.clover.reporters.filters.FileSetFilter;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;


public class Current extends CloverReportConfig {
    private boolean summary;

    protected static final String ERR_INITSTRING_NOT_SPECIFIED = "initstring not specified.";

    public static final Current DEFAULT_HTML = new Current(false);
    public static final Current DEFAULT_XML = new Current(false);
    public static final Current DEFAULT_PDF = new Current(true);

    /** Maximum number of tests per single source file. For unlimited use -1 */
    private int maxTestsPerFile = -1;

    /** Whether to take into account code coverage from failed tests */
    private boolean includeFailedTestCoverage = false;

    private List<File> testSourceFiles = newArrayList();

    protected List<File> testResultFiles;

    private List<String> globalFileNames = newArrayList();

    /** Number of threads to start when rendering reports */
    private int numThreads = 2;

    /** Timeout for report generation */
    private Interval timeOut = new Interval(Long.MAX_VALUE / 1000, Interval.UNIT_SECOND);

    /** Whether to calculate and show unique per-test coverage in the report */
    private boolean showUniqueCoverage = false;

    /** Whether to show lambda functions on a method list */
    private boolean showLambdaFunctions = false;

    /** Whether to show inner functions on a method list */
    private boolean showInnerFunctions = false;

    public Current() {
    }

    public Current(Current that) {
        this.summary = that.summary;
    }

    public Current(boolean summary) {
        this.summary = summary;
    }

    public void setSummary(boolean summary) {
        this.summary = summary;
    }

    public boolean getSummary() {
        return summary;
    }

    @Override
    public boolean validate() {
        if (!super.validate()) {
            return false;
        }

        if (getInitString() == null || getInitString().length() == 0) {
            setFailureReason(ERR_INITSTRING_NOT_SPECIFIED);
            return false;
        }

        // no format element specified
        if (getFormat() == null) {
            if (getSummary()) {
                setFormat(Format.DEFAULT_PDF);
            } else {
                setFormat(Format.DEFAULT_XML);
            }
        }

        // user has set a format element, but no type
        Format format = getFormat();
        if (format != null) {
            if (format.getType() == null) {
                setFailureReason("You need to set the report type");
                return false;
            }
            if (format.in(Type.PDF) && !getSummary()) {
                setFailureReason("Can only do summary reports in PDF. Use summary=\"true\" option.");
                return false;
            }
            if (format.in(Type.PDF) && (isShowInnerFunctions() || isShowLambdaFunctions())) {
                setFailureReason("The showInnerFunctions and/or showLambdaFunctions are not supported in PDF.");
                return false;
            }
        }

        if (numThreads < 0) {
            setFailureReason("numThreads must be greater than or equal to zero, not: " + numThreads);
            return false;
        }

        return true;
    }

    public String toString() {
        return "Current[" + getFormat().getType() + ", " + getOutFile().getAbsolutePath() + "]";
    }

    public void addGlobalFileName(String fileName) {
        globalFileNames.add(fileName);
    }

    public void addTestResultFile(File file) {
        if (testResultFiles == null) {
            testResultFiles = newArrayList();
        }
        testResultFiles.add(file);
    }

    public void addTestSourceFile(File file) {
        testSourceFiles.add(file);
    }

    public List<File> getTestSourceFiles() {
        return Collections.unmodifiableList(testSourceFiles);
    }

    public List<String> getGlobalSourceFileNames() {
        return Collections.unmodifiableList(globalFileNames);
    }

    @Override
    public CloverDatabase getCoverageDatabase() throws CloverException {
        setLoadTestResults(testResultFiles == null); // if a user has supplied TEST-*.xml, don't load built-in test results
        final CloverDatabase db = super.getCoverageDatabase();

        try {
            if (!isLoadTestResults() && db.getTestOnlyModel() != null) {
                TestResultProcessor.addTestResultsToModel(db.getTestOnlyModel(), testResultFiles);
            }
        } catch (CloverException e) {
            Logger.getInstance().error("Error parsing test results: " + e.getMessage() + " - Test results will not be included.");
        }

        boolean hasTestResult =  db.getTestOnlyModel() != null && db.getTestOnlyModel().hasTestResults();

        if (hasTestResult && !isIncludeFailedTestCoverage()) {
            final CoverageData data = db.getCoverageData();
            final CoverageDataProvider provider = new MaskedBitSetCoverageProvider(data.getPassOnlyAndIncidentalHits(), data, data);
            db.getAppOnlyModel().setDataProvider(provider);
            db.getTestOnlyModel().setDataProvider(provider);
            db.getFullModel().setDataProvider(provider);

            // set hasTestResults on the full model too
            db.getFullModel().setHasTestResults(hasTestResult);
        }

        if (isIncludeFailedTestCoverage() && !hasTestResult) {
            Logger.getInstance().debug("includeFailedTestCoverage='" + isIncludeFailedTestCoverage() +
                    "', however no test results were found.");
        }
        
        return db;
    }

    @Override
    protected HasMetricsFilter.Invertable effectiveTestFilter() {
        if (testSourceFiles.size() > 0) {
            return new FileSetFilter(testSourceFiles);
        } else {
            final HasMetricsFilter.Invertable filter = getTestFilter();
            return filter != null ? filter : new DefaultTestFilter();
        }
    }

    public int getMaxTestsPerFile() {
        return maxTestsPerFile;
    }

    public void setMaxTestsPerFile(int maxTestsPerFile) {
        this.maxTestsPerFile = maxTestsPerFile;
    }


    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public boolean isShowLambdaFunctions() {
        return showLambdaFunctions;
    }

    /**
     * Whether to present lambda functions {@link com.atlassian.clover.api.registry.MethodInfo#isLambda()}}
     * in the report. If set to <code>false</code> they are hidden on the list, but code metrics still include them.
     *
     * @param showLambdaFunctions true to show lambda functions
     */
    public void setShowLambdaFunctions(boolean showLambdaFunctions) {
        this.showLambdaFunctions = showLambdaFunctions;
    }

    public boolean isShowInnerFunctions() {
        return showInnerFunctions;
    }

    /**
     * Whether to show inner functions, i.e. functions declared inside methods in the report. This applies to Java8
     * lambda functions for instance. If set to <code>false</code> then they are hidden on the list, but code metrics
     * still include them.
     *
     * @param showInnerFunctions true to show inner functions
     */
    public void setShowInnerFunctions(boolean showInnerFunctions) {
        this.showInnerFunctions = showInnerFunctions;
    }

    public boolean isShowUniqueCoverage() {
        return showUniqueCoverage;
    }

    public void setShowUniqueCoverage(boolean showUniqueCoverage) {
        this.showUniqueCoverage = showUniqueCoverage;
    }

    public boolean isIncludeFailedTestCoverage() {
        return includeFailedTestCoverage;
    }

    public void setIncludeFailedTestCoverage(boolean includeFailedTestCoverage) {
        this.includeFailedTestCoverage = includeFailedTestCoverage;
    }

    public Interval getTimeOut() {
        return timeOut;
    }

    /**
     * The timeout to be used. You can use time units: s-second, m-minute, h-hour, d-day, w-week, mo-month, y-year.
     *
     * @param timeOutDescription interval like "10m"
     */
    public void setTimeOut(String timeOutDescription) {
        this.timeOut = new Interval(timeOutDescription);
    }
}


