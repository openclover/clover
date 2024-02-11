package org.openclover.eclipse.core.reports;

import org.openclover.core.cfg.Interval;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.model.InMemoryCurrentReportConfig;
import org.openclover.eclipse.core.settings.InstallationSettings;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.Format;
import org.openclover.core.reporters.ShowLambdaFunctions;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

public class GenerateReportWizard extends Wizard {

    SelectReportPage selectReportPage;
    SelectProjectsPage selectProjectsPage;
    ConfigureHtmlPage configureHtmlPage;
    ConfigurePdfPage configurePdfPage;
    ConfigureXmlPage configureXmlPage;
    ConfigureFilterPage configureFilterPage;
    ConfigureJvmPage configureJvmPage;
    CloverProject initiallySelectedProject;
    IWorkbench workbench;

    public GenerateReportWizard(CloverProject project, IWorkbench workbench) throws MalformedURLException {
        this.initiallySelectedProject = project;
        this.workbench = workbench;
        setWindowTitle("Generate Report");
        setDefaultPageImageDescriptor(
            CloverPlugin.getImageDescriptor(CloverPluginIcons.REPORT_WIZARD_ICON));
    }

    @Override
    public void addPages() {
        // initialise the wizards pages.
        selectReportPage = new SelectReportPage(initiallySelectedProject);
        selectProjectsPage = new SelectProjectsPage(initiallySelectedProject);
        configureHtmlPage = new ConfigureHtmlPage();
        configurePdfPage = new ConfigurePdfPage();
        configureXmlPage = new ConfigureXmlPage();
        configureFilterPage = new ConfigureFilterPage();
        configureJvmPage = new ConfigureJvmPage();

        addPage(selectReportPage);
        addPage(selectProjectsPage);
        addPage(configureHtmlPage);
        addPage(configurePdfPage);
        addPage(configureXmlPage);
        addPage(configureFilterPage);
        addPage(configureJvmPage);
    }

    
    /**
     * This wizard 'canFinish' if the following conditions are met.
     * <ul>
     * <li>The current page is not the starting page.</li>
     * <li>The current page is complete.</li>
     * </ul>
     *
     * @return
     */
    @Override
    public boolean canFinish() {
        IWizardPage currentPage = this.getContainer().getCurrentPage();
        if (currentPage == selectReportPage || currentPage == selectProjectsPage) {
            return false;
        }
        return (currentPage.isPageComplete());
    }

    /**
     * Generate the report as configured by the user.
     *
     * @return true if the report was successfully generated, false otherwise.
     */
    @Override
    public boolean performFinish() {

        final Shell shell = workbench.getActiveWorkbenchWindow().getShell();

        // check if output directory/file exists, and prompt user for overwrite
        // confirmation.
        boolean overwrite = true;
        if (selectReportPage.isHtmlSelected()) {
            File dir = configureHtmlPage.getOutput();
            if (new File(dir, "index.html").exists()) {
                overwrite = MessageDialog.openQuestion(shell, "Overwrite?",
                    "It appears that a report already exists. Do you want " +
                        "to overwrite it?");
            }
        } else if (selectReportPage.isPdfSelected()) {
            if (configurePdfPage.getOutput().exists()) {
                overwrite = MessageDialog.openQuestion(shell, "Overwrite?",
                    "A file with this name already exists. Do you want " +
                        "to overwrite it?");
            }
        } else if (selectReportPage.isXmlSelected()) {
            if (configureXmlPage.getOutput().exists()) {
                overwrite = MessageDialog.openQuestion(shell, "Overwrite?",
                    "A file with this name already exists. Do you want " +
                        "to overwrite it?");
            }
        }

        if (!overwrite) {
            return false;
        }

        //Need to load AWT on the SWT UI thread so as to avoid deadlock
        //if this occurs in another thread
        maybeInitialiseAWT();

        try {
            ReportJob reportJob = buildReportJob();
            reportJob.setUser(true);
            reportJob.addJobChangeListener(new ReportJobListener(shell));
            reportJob.schedule();
        } catch (IOException e) {
            CloverPlugin.logError("Unable to generate report", e);
        }

        return true;
    }

    private void maybeInitialiseAWT() {
        if (CloverPlugin.getInstance().getInstallationSettings().getBoolean(InstallationSettings.Keys.PRIME_AWT_IN_UI_THREAD)) {
            Toolkit.getDefaultToolkit();
        }
    }


    private ReportJob buildReportJob() throws IOException {
        if (selectProjectsPage.getSelectedProjects().length == 1) {
            if (getReportTypeWizard().isUsingDefaultContextFilter()) {
                return buildDefaultFilteredReportJob();
            }
            else {
                return buildSpeciallyFilteredReportJob();
            }
        } else {
            return buildMergeFirstReportJob();
        }
    }

    private ReportJob buildSpeciallyFilteredReportJob() {
        final CloverProject cloverProject = selectProjectsPage.getSelectedProjects()[0];
        return new FilteredReportJob(
                cloverProject,
                buildFilteredReportConfig(
                        newCurrent(),
                        cloverProject,
                        configureFilterPage.getContextRegistry().getContextsAsString(
                                configureFilterPage.getBlockContextFilter())),
                configureJvmPage.getJvmArgs(),
                configureJvmPage.getMaxHeapSize());
    }

    private Current newCurrent() {
        if (selectReportPage.isHtmlSelected()) {
            return new Current(Current.DEFAULT_HTML);
        } else if (selectReportPage.isPdfSelected()) {
            return new Current(Current.DEFAULT_PDF);
        } else {
            return new Current(Current.DEFAULT_XML);
        }
    }

    private Current buildFilteredReportConfig(Current reportConfig, CloverProject project, String filter) {
        if (selectReportPage.isHtmlSelected()) {
            primeForHtmlReport(reportConfig);
        } else if (selectReportPage.isPdfSelected()) {
            primeForPdfReport(reportConfig);
        } else {
            primeForXmlReport(reportConfig);
        }
        reportConfig.getFormat().setFilter(filter);
        reportConfig.setInitString(project.deriveInitString());

        return reportConfig;
    }

    private ReportJob buildMergeFirstReportJob() throws IOException {
        return new MergeReportJob(
            selectProjectsPage.getSelectedProjects(),
            buildMergeReportConfig(),
            configureJvmPage.getJvmArgs(),
            configureJvmPage.getMaxHeapSize());
    }

    private Current buildMergeReportConfig() throws IOException {
        Current reportConfig = null;
        if (selectReportPage.isHtmlSelected()) {
            reportConfig = new Current(Current.DEFAULT_HTML);
            primeForHtmlReport(reportConfig);
        } else if (selectReportPage.isPdfSelected()) {
            reportConfig = new Current(Current.DEFAULT_PDF);
            primeForPdfReport(reportConfig);
        } else {
            reportConfig = new Current(Current.DEFAULT_XML);
            primeForXmlReport(reportConfig);
        }
        reportConfig.getFormat().setFilter(
            configureFilterPage.getContextRegistry().getContextsAsString(configureFilterPage.getBlockContextFilter()));
        File mergedDb = File.createTempFile("clover", "merge");
        mergedDb.deleteOnExit();
        reportConfig.setInitString(mergedDb.getAbsolutePath());
        return reportConfig;
    }

    private ReportJob buildDefaultFilteredReportJob() {
        CloverProject project = selectProjectsPage.getSelectedProjects()[0];

        return new FilteredReportJob(
                project,
                buildFilteredReportConfig(
                        newInMemoryReportConfig(project),
                        project,
                        contextFilterAsString(project)),
                configureJvmPage.getJvmArgs(),
                configureJvmPage.getMaxHeapSize());
    }

    private String contextFilterAsString(CloverProject project) {
        return project.getModel().getDatabase().getContextStore().getContextsAsString(
            project.getSettings().getContextFilter());
    }

    private InMemoryCurrentReportConfig newInMemoryReportConfig(CloverProject project) {
        if (selectReportPage.isHtmlSelected()) {
            return new InMemoryCurrentReportConfig(project.getModel(), Current.DEFAULT_HTML);
        } else if (selectReportPage.isPdfSelected()) {
            return new InMemoryCurrentReportConfig(project.getModel(), Current.DEFAULT_PDF);
        } else {
            return new InMemoryCurrentReportConfig(project.getModel(), Current.DEFAULT_XML);
        }
    }

    private void primeForReport(Current reportConfig) {
        reportConfig.setAlwaysReport(true);
        reportConfig.setSpan(calculateSpan());
    }

    private Interval calculateSpan() {
        CloverProject[] selectedProjects = selectProjectsPage.getSelectedProjects();
        if (selectedProjects.length > 1) {
            //Merge takes care of spans for individual projects, so
            //no report span if merge is going to take place
            return Interval.ZERO_INTERVAL;
        } else {
            //TODO: selectedProjects[0].getModel().getDatabase() could be null if db failed to load
            return selectedProjects[0].getSettings().calcEffectiveSpanInterval(selectedProjects[0].getModel().getDatabase());
        }
    }

    private void primeForXmlReport(final Current reportConfig) {
        reportConfig.setFormat(new Format(Format.DEFAULT_XML));
        reportConfig.setOutFile(configureXmlPage.getOutput());
        reportConfig.setTitle(configureXmlPage.getReportTitle());
        reportConfig.setNumThreads(configureXmlPage.getNumThreads());
        reportConfig.getFormat().setSrcLevel(configureXmlPage.isIncludingLineInfo());
        reportConfig.setShowLambdaFunctions(configureXmlPage.getShowLambdaFunctions() != ShowLambdaFunctions.NONE);
        reportConfig.setShowInnerFunctions(configureXmlPage.getShowLambdaFunctions() == ShowLambdaFunctions.FIELDS_AND_INLINE);
        primeForReport(reportConfig);
    }

    private void primeForPdfReport(final Current reportConfig) {
        reportConfig.setFormat(new Format(Format.DEFAULT_PDF));
        reportConfig.setOutFile(configurePdfPage.getOutput());
        reportConfig.setTitle(configurePdfPage.getReportTitle());
        reportConfig.setNumThreads(configurePdfPage.getNumThreads());
        primeForReport(reportConfig);
    }

    private void primeForHtmlReport(final Current reportConfig) {
        reportConfig.setFormat(new Format(Format.DEFAULT_HTML));
        reportConfig.setOutFile(configureHtmlPage.getOutput());
        reportConfig.setMainFileName("index.html");
        reportConfig.setTitle(configureHtmlPage.getReportTitle());
        reportConfig.setNumThreads(configureHtmlPage.getNumThreads());
        reportConfig.getFormat().setSrcLevel(configureHtmlPage.shouldIncludeSource());
        reportConfig.setIncludeFailedTestCoverage(configureHtmlPage.shouldIncludeFailedTestCoverage());
        reportConfig.setShowLambdaFunctions(configureHtmlPage.getShowLambdaFunctions() != ShowLambdaFunctions.NONE);
        reportConfig.setShowInnerFunctions(configureHtmlPage.getShowLambdaFunctions() == ShowLambdaFunctions.FIELDS_AND_INLINE);
        primeForReport(reportConfig);
    }

    protected boolean useDefaultFilter() {
        return getReportTypeWizard().isUsingDefaultContextFilter();
    }

    protected ConfigureReportPage getReportTypeWizard() {
        if (selectReportPage.isHtmlSelected()) {
            return configureHtmlPage;
        } else if (selectReportPage.isPdfSelected()) {
            return configurePdfPage;
        } else {
            return configureXmlPage;
        }
    }

}
