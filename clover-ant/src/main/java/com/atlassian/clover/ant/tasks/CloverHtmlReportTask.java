package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.reporters.Format;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * A user friendly report task.
 */
public class CloverHtmlReportTask extends CloverReportTask {

    private String title = "";
    private File outdir;
    private File historyoutfile;

    private final CurrentEx current = new CurrentEx();

    private File historyDir;
    private String historyIncludes;
    private boolean showUniqueCoverage = false;

    public void setOutdir(File outdir) {
        this.outdir = outdir;
        this.historyoutfile = outdir;
    }

    public void setHistorydir(File dir) {
        historyDir = dir;
    }

    public void setHistoryIncludes(String patternSpec) {
       historyIncludes = patternSpec;
    }

    public void setShowUniqueCoverage(boolean show) {
        this.showUniqueCoverage = show;
    }

    public void setTestResultsDir(File testResultDir) {
        FileSet resultsFileSet = new FileSet();
        resultsFileSet.setDir(testResultDir);
        resultsFileSet.setIncludes("TEST*.xml");
        current.addTestResults(resultsFileSet);
    }

    public void addColumns(Columns columns) {
        current.setColumns(columns);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMaxTestsPerFile(int max) {
        current.setMaxTestsPerFile(max);
    }

    public void setNumThreads(int threads) {
        current.setNumThreads(threads);
    }

    @Override
    public void cloverExecute() {
        if (outdir == null) {
            throw new BuildException("'outdir' attribute is required.");
        }
        current.setFormat(getFormat());
        current.setTitle(title);
        current.setOutFile(outdir);
        current.setCoverageCacheSize(coverageCacheSize);
        current.setShowUniqueCoverage(showUniqueCoverage);
        addCurrent(current);

        if (historyDir != null) {
            HistoricalEx historical = new HistoricalEx();
            historical.setHistoryDir(historyDir);
            if (historyIncludes != null) {
                historical.setHistoryIncludes(historyIncludes);
            }
            
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
            
            if (!current.getTestResults().isEmpty()) {
                hptask.addTestResults(current.getTestResults().get(0));
            }

            hptask.cloverExecute();

            historical.setFormat(getFormat());
            historical.setTitle(title);
            historical.setOutFile(historyoutfile);
            historical.resolve(getProject());
            addHistorical(historical);
        }
        super.cloverExecute();
    }

    protected Format getFormat() {
        return Format.DEFAULT_HTML;
    }

}
