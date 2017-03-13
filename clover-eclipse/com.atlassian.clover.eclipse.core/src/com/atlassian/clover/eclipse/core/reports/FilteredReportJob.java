package com.atlassian.clover.eclipse.core.reports;

import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.CloverReporter;
import com.atlassian.clover.reporters.html.HtmlReporter;
import com.atlassian.clover.reporters.pdf.PDFReporter;
import com.atlassian.clover.reporters.xml.XMLReporter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class FilteredReportJob extends ForkingReportJob {
    private final CloverProject cloverProject;

    public FilteredReportJob(CloverProject cloverProject, Current reportConfig, String vmArgs, String mxSetting) {
        super(reportConfig, vmArgs, mxSetting);
        this.cloverProject = cloverProject;
    }

    @Override
    protected IStatus runReporter(IProgressMonitor monitor) throws Exception {
        CloverReporter.buildReporter(config).execute();
        return Status.OK_STATUS;
    }

    protected String calculateInitString() {
        return config.getInitString();
    }

    @Override
    protected String calculateReporterClass() {
        switch (config.getFormat().getType()) {
            case HTML:
                return FilteredHtmlReporter.class.getName();
            case PDF:
                return FilteredPDFReporter.class.getName();
            default:
                return FilteredXMLReporter.class.getName();
        }
    }

    @Override
    protected String calcTestFilterArgs() {
        return calcTestFilterArgs(cloverProject);
    }

    public static class FilteredHtmlReporter extends ForkingReporter {
        @Override
        protected int run(String[] args) {
            return HtmlReporter.runReport(args);
        }
    }

    public static class FilteredXMLReporter extends ForkingReporter {
        @Override
        protected int run(String[] args) {
            return XMLReporter.runReport(args);
        }
    }

    public static class FilteredPDFReporter extends ForkingReporter {
        @Override
        protected int run(String[] args) {
            return PDFReporter.runReport(args);
        }
    }
}
