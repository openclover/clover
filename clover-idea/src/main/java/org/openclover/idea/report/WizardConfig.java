package org.openclover.idea.report;

import com.atlassian.clover.reporters.ShowLambdaFunctions;
import org.openclover.idea.IDEContext;

import java.io.File;

/**
 * Configuration used by Report Wizard.
 */
public class WizardConfig {
    abstract class AbstractReport {

        private String reportTitle = null;

        private ShowLambdaFunctions showLambda = ShowLambdaFunctions.NONE;

        public String getReportTitle() {
            return reportTitle;
        }

        public void setReportTitle(String s) {
            this.reportTitle = s;
        }

        public ShowLambdaFunctions getShowLambda() {
            return showLambda;
        }

        public void setShowLambda(ShowLambdaFunctions showLambda) {
            this.showLambda = showLambda;
        }

    }

    /**
     * Configuration for HTML report.
     */
    class HtmlReport extends AbstractReport {

        private File dir = null;
        private boolean includeSource = false;
        private boolean includeFailedCoverage = false;

        /**
         * Returns output directory for the report.
         * @return
         */
        public File getDir() {
            return dir;
        }

        /**
         * Sets output directory for the report.
         * @param dir
         */
        public void setDir(File dir) {
            this.dir = dir;
        }

        /**
         * Returns whether report should contain source code. If yes - coloured sources will be attached,
         * if not - just a short metrics summary for each package/class.
         * @return boolean
         */
        public boolean isIncludeSource() {
            return includeSource;
        }

        /**
         * Sets whether report should contain source code.
         * @param included
         */
        public void setIncludeSource(boolean included) {
            this.includeSource = included;
        }

        /**
         * Sets whether code coverage from failed tests shall be considered in the report.
         * @param included
         */
        public void setIncludeFailedTestCoverage(boolean included) {
            this.includeFailedCoverage = included;
        }

        /**
         * Returns whether code coverage from failed tests shall be considered in the report.
         * @return boolean
         */
        public boolean isIncludeFailedCoverage() {
            return includeFailedCoverage;
        }
    }

    /**
     * Configuration for PDF report.
     */
    class PdfReport extends AbstractReport {

        private File file = null;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }
    }

    /**
     * Configuration for PDF report.
     */
    class XmlReport extends AbstractReport {

        private File file = null;
        private boolean includeLineInfo = false;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public boolean isIncludeLineInfo() {
            return includeLineInfo;
        }

        public void setIncludeLineInfo(boolean b) {
            this.includeLineInfo = b;
        }
    }

    private final HtmlReport htmlConfig = new HtmlReport();
    private final PdfReport pdfConfig = new PdfReport();
    private final XmlReport xmlConfig = new XmlReport();

    private int type;

    public static final int HTML = 1;
    public static final int PDF = 2;
    public static final int XML = 3;

    private boolean useCurrentFilterSettings;
    private String contextSpec;


    public WizardConfig(IDEContext context) {

        String title = (context.getProjectName() != null) ? context.getProjectName() + " Coverage Report" : "Coverage Report";

        xmlConfig.setReportTitle(title);
        xmlConfig.setFile(new File(context.getProjectRootDirectory(), "report/coverage.xml"));
        xmlConfig.setShowLambda(ReportWizardWorkspaceSettings.getInstance().getShowLambda());

        pdfConfig.setReportTitle(title);
        pdfConfig.setFile(new File(context.getProjectRootDirectory(), "report/coverage.pdf"));

        htmlConfig.setReportTitle(title);
        htmlConfig.setDir(new File(context.getProjectRootDirectory(), "report/html"));

        // load some of settings from IDEA user preferences storage
        htmlConfig.setIncludeSource(ReportWizardWorkspaceSettings.getInstance().isIncludeSources());
        htmlConfig.setIncludeFailedTestCoverage(ReportWizardWorkspaceSettings.getInstance().isIncludeFailedCoverage());
        htmlConfig.setShowLambda(ReportWizardWorkspaceSettings.getInstance().getShowLambda());
    }

    public HtmlReport getHtmlConfig() {
        return htmlConfig;
    }

    public PdfReport getPdfConfig() {
        return pdfConfig;
    }

    public XmlReport getXmlConfig() {
        return xmlConfig;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public boolean isHtml() {
        return type == HTML;
    }

    public boolean isPdf() {
        return type == PDF;
    }

    public boolean isXml() {
        return type == XML;
    }

    public boolean isUseCurrentFilterSettings() {
        return useCurrentFilterSettings;
    }

    public void setUseCurrentFilterSettings(boolean b) {
        this.useCurrentFilterSettings = b;
    }

    public String getContextSpec() {
        return contextSpec;
    }

    public void setContextSpec(String contextSpec) {
        this.contextSpec = contextSpec;
    }
}

