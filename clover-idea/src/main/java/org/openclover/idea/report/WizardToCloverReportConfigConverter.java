package org.openclover.idea.report;

import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.Format;
import com.atlassian.clover.reporters.ShowLambdaFunctions;

/**
 * Converts WizardConfig to CloverReportConfig.
 */
public class WizardToCloverReportConfigConverter {

    /**
     * Converts WizardConfig to CloverReportConfig (or one of its subclasses) so that it can be usable
     * by one of reporters.
     *
     * @param wizardConfig         configuration returned by ReportWizard
     * @param defaultContextSpec   default context filter settings which has to be used if user has
     *                             chosen "use current filter settings" in dialog
     * @return CloverReportConfig
     */
    public static CloverReportConfig convert(WizardConfig wizardConfig, String defaultContextSpec) {
        final Current reportConfig;

        switch (wizardConfig.getType()) {
            case WizardConfig.HTML:
                final WizardConfig.HtmlReport htmlConfig = wizardConfig.getHtmlConfig();
                reportConfig = new Current(Current.DEFAULT_HTML);
                reportConfig.setFormat(Format.DEFAULT_HTML);
                reportConfig.setMainFileName("index.html");
                reportConfig.setOutFile(htmlConfig.getDir());
                reportConfig.setTitle(htmlConfig.getReportTitle());
                reportConfig.getFormat().setSrcLevel(htmlConfig.isIncludeSource());
                reportConfig.setIncludeFailedTestCoverage(htmlConfig.isIncludeFailedCoverage());
                reportConfig.setShowLambdaFunctions(htmlConfig.getShowLambda() != ShowLambdaFunctions.NONE);
                reportConfig.setShowInnerFunctions(htmlConfig.getShowLambda() == ShowLambdaFunctions.FIELDS_AND_INLINE);
                break;
            case WizardConfig.PDF:
                final WizardConfig.PdfReport pdfConfig = wizardConfig.getPdfConfig();
                reportConfig = new Current(Current.DEFAULT_PDF);
                reportConfig.setFormat(Format.DEFAULT_PDF);
                reportConfig.setOutFile(pdfConfig.getFile());
                reportConfig.setTitle(pdfConfig.getReportTitle());
                break;
            case WizardConfig.XML:
                final WizardConfig.XmlReport xmlConfig = wizardConfig.getXmlConfig();
                reportConfig = new Current(Current.DEFAULT_XML);
                reportConfig.setFormat(Format.DEFAULT_XML);
                reportConfig.setOutFile(xmlConfig.getFile());
                reportConfig.setTitle(xmlConfig.getReportTitle());
                reportConfig.getFormat().setSrcLevel(xmlConfig.isIncludeLineInfo());
                reportConfig.setShowLambdaFunctions(xmlConfig.getShowLambda() != ShowLambdaFunctions.NONE);
                reportConfig.setShowInnerFunctions(xmlConfig.getShowLambda() == ShowLambdaFunctions.FIELDS_AND_INLINE);
                break;
            default:
                throw new IllegalStateException("Invalid report type: " + wizardConfig.getType());
        }

        reportConfig.setAlwaysReport(true);

        String contextSpec = wizardConfig.getContextSpec();
        if (wizardConfig.isUseCurrentFilterSettings()) {
            // use current filter settings.
            reportConfig.getFormat().setFilter(defaultContextSpec);
        } else if (contextSpec != null) {
            // use filter which was set via the "ConfigureFilterUI" page
            reportConfig.getFormat().setFilter(contextSpec);
        } else {
            // use no filters at all
            reportConfig.getFormat().setFilter("");
        }

        return reportConfig;
    }
}
