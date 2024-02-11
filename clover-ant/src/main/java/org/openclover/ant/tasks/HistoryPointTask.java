package org.openclover.ant.tasks;

import org.openclover.ant.AntLogger;
import com.atlassian.clover.CloverDatabase;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.Logger;
import org.openclover.runtime.CloverNames;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.reporters.Format;
import com.atlassian.clover.reporters.xml.XMLReporter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Ant support for the history-point clover functionality.
 * <p>A history-point is a snapshot of the coverage at a particular point in
 * time. Multiple history points can then be used to generate a historical graph
 * plotting the changes in coverage over time.
 *
 */
public class HistoryPointTask extends AbstractCloverTask {

    private File historyDir;
    private String dateString;
    private String dateFormat;
    private Interval span = Interval.DEFAULT_SPAN;
    private String filter;
    private String property;
    private boolean overwrite = false;
    
    private final CloverReportTask.CurrentEx config = new CloverReportTask.CurrentEx();
    private boolean alwaysReport = false;
    private boolean srcLevel = true;

    /**
     * Sepcify the directory into which the history information will be written.
     */
    public void setHistoryDir(File historyDir) {
        this.historyDir = historyDir;
    }

    /**
     * Specify the effective date for this history point.
     */
    public void setDate(String dateString) {
        this.dateString = dateString;
    }

    /**
     * Specify the format of the date property. Defaults to the default SimpleDateFormat
     * @see HistoryPointTask#setDate(java.lang.String)
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * Specify the span to be used for generating this history-point
     */
    public void setSpan(Interval span) {
        this.span = span;
    }

    /**
     * Specifies the property name to hold the name of the generated output file
     * @param property the name of the property to hold the filename that was generated
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Support the use of filesets to filter the history point coverage.
     */
    public void addFileSet(FileSet fileset) {
        config.addFileSet(fileset);
    }


    public void addTestSources(FileSet fileset) {
        config.addTestSources(fileset);
    }

    public void addTestResults(FileSet fileset) {
        config.addTestResults(fileset);
    }

    public void setIncludeFailedTestCoverage(boolean include) {
        config.setIncludeFailedTestCoverage(include);
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setAlwaysReport(boolean alwaysReport) {
        this.alwaysReport = alwaysReport;
    }


    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setSrcLevel(boolean srcLevel) {
        this.srcLevel = srcLevel;
    }

    /**
     *
     */
    @Override
    public void cloverExecute() {
        String initString = resolveInitString();

        if (historyDir == null) {
            throw new BuildException("You must specify a directory for the "
                    + "historical information using the \"historydir\" attribute");
        }

        SimpleDateFormat format;
        if (dateFormat == null) {
            format = new SimpleDateFormat();
        } else {
            try {
                format = new SimpleDateFormat(dateFormat);
            } catch (IllegalArgumentException e) {
                throw new BuildException("The format \"" + dateFormat
                        + "\" is not a valid date format", e);
            }
        }

        historyDir.mkdirs();
        Date reportDate = null;
        if (dateString != null) {
            try {
                reportDate = format.parse(dateString);
            } catch (ParseException e) {
                throw new BuildException("The date \"" + dateString
                        + "\" is not valid according to the date format \""
                        + format.toPattern() + "\"", e);
            }
        }

        Format fmt = new Format(Format.DEFAULT_XML);
        if (filter != null) {
            fmt.setFilter(filter); 
        }
        fmt.setSrcLevel(srcLevel);
        config.setFormat(fmt);
        config.setInitString(initString);
        config.setEffectiveDate(reportDate);
        config.setSpan(span);
        config.setAlwaysReport(alwaysReport);
        config.setLoadPerTestData(!config.isIncludeFailedTestCoverage());
        if (config.getFilesets().size() > 0) {
            config.setIncludeFilter(new FilesetFilter(getProject(), config.getFilesets()));
        }
        Logger.setInstance(new AntLogger(getProject(), this));
        config.resolve(getProject());

        try {
            CloverReportTask.checkTestSourceFileSet(config.getFilesets(), config);            
            CloverDatabase model = config.getCoverageDatabase();
            if (reportDate == null) {
                reportDate = new Date(model.getRecordingTimestamp());
            }
            SimpleDateFormat formatter
                    = new SimpleDateFormat("yyyyMMddHHmmss");
            String tag = formatter.format(reportDate);

            File outfile = new File(historyDir, CloverNames.HISTPOINT_PREFIX + tag + CloverNames.HISTPOINT_SUFFIX);

            if (outfile.exists() && !overwrite) {
               Logger.getInstance().warn("Not overwriting existing history point '" + outfile.getAbsolutePath() + "'. To force, set overwrite=\"true\".");
               return;
            }

            if (property != null) {
                getProject().setProperty(property, outfile.getAbsolutePath());
            }
            config.setOutFile(outfile);
            config.setCompress(true);

            new XMLReporter(model, config).execute();
        }
        catch (CloverException e) {
            throw new BuildException(e);
        }
    }
}
