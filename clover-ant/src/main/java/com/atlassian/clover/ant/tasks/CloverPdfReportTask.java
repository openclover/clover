package com.atlassian.clover.ant.tasks;

import clover.com.google.common.collect.Lists;
import com.atlassian.clover.reporters.Format;
import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.pdf.PDFReporter;

import com.atlassian.clover.api.CloverException;

import java.io.File;
import java.util.Map;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;

import static clover.com.google.common.collect.Maps.newTreeMap;

public class CloverPdfReportTask extends CloverReportTask {
    private String title = "";
    private File outfile;
    private final CurrentEx current = new CurrentEx();
    private HistoricalEx historical;

    public void setHistorydir(File historydir) {
        historical = new HistoricalEx();
        historical.setHistoryDir(historydir);
    }

    public void addColumns(Columns columns) {
        current.setColumns(columns);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOutfile(File outfile) {
        this.outfile = outfile;
    }

    @Override
    public void cloverExecute() {
        if (outfile == null) {
            throw new BuildException("'outfile' attribute is required.");
        }
        current.setFormat(getFormat());
        current.setTitle(title);
        current.setOutFile(outfile);
        current.setSummary(true);
        current.setCoverageCacheSize(coverageCacheSize);
        addCurrent(current);

        if (historical != null) {
            if (!historical.getHistoryDir().exists() && !historical.getHistoryDir().mkdirs()) {
                 throw new BuildException("Unable to create history dir '" + historical.getHistoryDir() + "'");
            }
            HistoryPointTask hptask = new HistoryPointTask();
            hptask.setProject(getProject());
            hptask.init();
            hptask.setHistoryDir(historical.getHistoryDir());
            hptask.setInitString(resolveInitString());
            hptask.setAlwaysReport(true);
            hptask.setTaskName(getTaskName());
            hptask.cloverExecute();

            historical.setFormat(getFormat());
            historical.setTitle(title);
            historical.setOutFile(outfile);
            addHistorical(historical);
        }
        super.cloverExecute();
    }

    protected Format getFormat() {
        return Format.DEFAULT_PDF;
    }

    @Override
    protected void generateReports(CloverReportConfig firstCurrentConfig, CloverReportConfig[] configs,
                                   Map<String, CloverReportConfig> linkedReports) throws CloverException {

        final CloverReportConfig config = configs[0];
        final Map<String, CloverReportConfig> myLinkedReports = newTreeMap();
        myLinkedReports.putAll(linkedReports);// copy all
        myLinkedReports.remove(config.getUniqueTitle()); // remove this report from the links

        final ArrayList<CloverReportConfig> secondaryReports = Lists.newArrayList(myLinkedReports.values());
        if (!config.validate()) {
            throw new CloverException(config.getValidationFailureReason());
        }

        new PDFReporter(config, secondaryReports.toArray(new CloverReportConfig[0])).execute();
    }
}
