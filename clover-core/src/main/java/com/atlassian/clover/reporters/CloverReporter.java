package com.atlassian.clover.reporters;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CloverStartup;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.reporters.html.HtmlReportUtil;
import com.atlassian.clover.reporters.html.HtmlReporter;
import com.atlassian.clover.reporters.json.JSONReporter;
import com.atlassian.clover.reporters.pdf.PDFReporter;
import com.atlassian.clover.reporters.xml.XMLReporter;

/**
 * A class that allows production of all the "hardcopy" clover reports
 */
public abstract class CloverReporter {

    protected static void loadLicense() {
        CloverStartup.loadLicense(Logger.getInstance());
    }

    protected final CloverDatabase database;
    protected final CloverReportConfig reportConfig;

    protected CloverReporter(CloverDatabase database, CloverReportConfig reportConfig) {
        this.database = database;
        this.reportConfig = reportConfig;
    }

    public CloverReporter(CloverReportConfig reportConfig) throws CloverException {
        this(reportConfig.getCoverageDatabase(), reportConfig);
    }

    public final int execute() throws CloverException {
        validate();
        return executeImpl();
    }

    protected void validate() throws CloverException {
        if (!reportConfig.validate()) {
            throw new CloverException(reportConfig.getValidationFailureReason());
        }

        if (database == null && !isHistoricalReport()) {
            //No initstring supplied but is current
            throw new CloverException("You must supply an initstring for a current PDF report");
        }
    }

    protected abstract int executeImpl() throws CloverException;

    protected static boolean canProceedWithReporting(CloverReportConfig config) {
        return config != null && HtmlReportUtil.getVelocityEngine() != null;
    }

    public static CloverReporter buildReporter(final CloverReportConfig config) throws CloverException {
        if (!config.validate()) {
            throw new CloverException(config.getValidationFailureReason());
        }

        switch (config.getFormat().getType()) {
            case PDF: return new PDFReporter(config);
            case XML: return new XMLReporter(config);
            case JSON: return new JSONReporter(config);
            default: return new HtmlReporter(config);
        }
    }

    protected boolean isCurrentReport() {
        return reportConfig instanceof Current;
    }

    protected boolean isHistoricalReport() {
        return reportConfig instanceof Historical;
    }
}
