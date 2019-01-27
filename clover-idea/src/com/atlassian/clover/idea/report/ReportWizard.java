package com.atlassian.clover.idea.report;

import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.idea.IDEContext;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.idea.util.ui.MessageDialogs;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.CloverReporter;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Window;
import java.io.File;
import java.text.MessageFormat;
import java.util.Stack;

public class ReportWizard {

    private static final Logger LOG = Logger.getInstance(ReportWizard.class.getName());

    public static final int FINISHED_EXIT_CODE = 0;
    public static final int CANCELLED_EXIT_CODE = 1;

    private final Stack<ReportWizardPage> previousPageStack = new Stack<ReportWizardPage>();

    private final String defaultContextSpec;

    private WizardConfig wizardConfig;

    private ReportWizardDialog dialog;

    public ReportWizard(IDEContext context) {
        defaultContextSpec = context.getContextFilterSpec();

        dialog = new ReportWizardDialog(this);

        wizardConfig = new WizardConfig(context);

        if (isHtmlAvailable()) {
            wizardConfig.setType(WizardConfig.HTML);
        } else if (isPdfAvailable()) {
            wizardConfig.setType(WizardConfig.PDF);
        } else if (isXmlAvailable()) {
            wizardConfig.setType(WizardConfig.XML);
        }

        SelectReportUI firstPage = new SelectReportUI(this);
        Icon[] fileTypes = context.getFileTypes();
        if (fileTypes != null && fileTypes.length == 3) {
            firstPage.setHtmlFileTypeIcon(fileTypes[0]);
            firstPage.setPdfFileTypeIcon(fileTypes[1]);
            firstPage.setXmlFileTypeIcon(fileTypes[2]);
        }
        firstPage.readConfig(wizardConfig);
        dialog.setCurrentPage(firstPage);

        refreshState();
    }

    public Window getDialogWindow() {
        return dialog.getOwner();
    }

    public CloverReportConfig getReportConfiguration() {
        final CloverReportConfig reportConfig = WizardToCloverReportConfigConverter.convert(wizardConfig, defaultContextSpec);
        // if the parent directory does not exist, then we need to create it, else report generation will fail
        reportConfig.getOutFile().getParentFile().mkdirs();
        return reportConfig;
    }

    private void refreshState() {
        refreshState(hasNext());
    }

    void refreshState(boolean nextEnabled) {
        dialog.getPreviousAction().setEnabled(previousPageStack.size() > 0);
        dialog.getNextAction().setEnabled(nextEnabled);
        dialog.getFinishAction().setEnabled(canFinish());

    }

    private boolean hasNext() {
        ReportWizardPage currentPage = dialog.getCurrentPage();
        if (currentPage instanceof ConfigureFilterUI) {
            return false;
        }
        if (currentPage instanceof ConfigureHtmlUI ||
                currentPage instanceof ConfigurePdfUI ||
                currentPage instanceof ConfigureXmlUI) {
            return !wizardConfig.isUseCurrentFilterSettings();
        }
        return true;
    }

    private boolean canFinish() {
        return !(dialog.getCurrentPage() instanceof SelectReportUI);
    }


    /**
     * Permissions check - is 'Current XML' reporting available to this license
     * holder.
     *
     * @return true if xml reporting is available.
     */
    protected boolean isXmlAvailable() {
        return true;
    }

    /**
     * Permissions check - is 'Current HTML' reporting available to this license
     * holder.
     *
     * @return true if html reporting is available.
     */
    protected boolean isHtmlAvailable() {
        return true;
    }

    /**
     * Permissions check - is 'Current PDF' reporting available to this license
     * holder.
     *
     * @return true if pdf reporting is available.
     */
    protected boolean isPdfAvailable() {
        return true;
    }

    /**
     *
     */
    protected void doNext() {

        ReportWizardPage currentPage = dialog.getCurrentPage();

        // Validate current page. If all is okay, then we can proceed.
        String validationErrorMessage = currentPage.validateSettings();
        if (validationErrorMessage != null) {
            MessageDialogs.showErrorMessage(null, validationErrorMessage, "Validation Error.");
            return;
        }


        currentPage.writeConfig(wizardConfig);
        previousPageStack.push(currentPage);

        // Determine the next page.
        final ReportWizardPage nextPage;
        if (currentPage instanceof SelectReportUI) {
            switch (wizardConfig.getType()) {
                case WizardConfig.HTML:
                    nextPage = new ConfigureHtmlUI(this);
                    break;
                case WizardConfig.PDF:
                    nextPage = new ConfigurePdfUI(this);
                    break;
                case WizardConfig.XML:
                    nextPage = new ConfigureXmlUI(this);
                    break;
                default:
                    throw new IllegalStateException("Invalid config type: " + wizardConfig.getType());
            }
        } else if (!(currentPage instanceof ConfigureFilterUI)) {
            nextPage = new ConfigureFilterUI(this);
        } else {
            throw new IllegalStateException("Invalid wizard state: currentPage is " + currentPage.getClass());
        }

        if (nextPage instanceof ConfigureFilterUI) {
            // handling transition to filter page.
            // if filter values have not been initialised, set them up now.
            if (wizardConfig.isUseCurrentFilterSettings()) {
                wizardConfig.setContextSpec(defaultContextSpec);
            } else {
                wizardConfig.setContextSpec("");
            }
        }

        nextPage.readConfig(wizardConfig);
        nextPage.validate();

        dialog.setCurrentPage(nextPage);

        refreshState();
    }

    protected void doPrevious() {
        dialog.getCurrentPage().writeConfig(wizardConfig);

        ReportWizardPage previousPage = previousPageStack.pop();
        previousPage.readConfig(wizardConfig);
        dialog.setCurrentPage(previousPage);


        refreshState();
    }

    private static final MessageFormat HTML_EXISTS_MESSAGE = new MessageFormat(
            "It appears that a report already exists in {0}.\nDo you want to overwrite it?");
    private static final MessageFormat FILE_EXISTS_MESSAGE = new MessageFormat(
            "A report already exists at {0}.\nDo you want to overwrite it?");

    protected void doFinish() {

        ReportWizardPage currentPage = dialog.getCurrentPage();
        currentPage.writeConfig(wizardConfig);

        // remember settings from wizard dialogs
        ReportWizardWorkspaceSettings.getInstance().setIncludeSources(wizardConfig.getHtmlConfig().isIncludeSource());
        ReportWizardWorkspaceSettings.getInstance().setIncludeFailedCoverage(wizardConfig.getHtmlConfig().isIncludeFailedCoverage());
        ReportWizardWorkspaceSettings.getInstance().setShowLambda(wizardConfig.getHtmlConfig().getShowLambda());

        // need to prompt the user whether or not they want to overwrite
        // existing files. if not, then do not exit.

        final File reportFile;
        final String existsMessage;
        switch (wizardConfig.getType()) {
            case WizardConfig.HTML:
                final File htmlDir = wizardConfig.getHtmlConfig().getDir();
                reportFile = new File(htmlDir, "index.html");
                existsMessage = HTML_EXISTS_MESSAGE.format(new String[]{htmlDir.getAbsolutePath()});
                break;
            case WizardConfig.PDF:
                reportFile = wizardConfig.getPdfConfig().getFile();
                existsMessage = FILE_EXISTS_MESSAGE.format(new String[]{reportFile.getAbsolutePath()});
                break;
            case WizardConfig.XML:
                reportFile = wizardConfig.getXmlConfig().getFile();
                existsMessage = FILE_EXISTS_MESSAGE.format(new String[]{reportFile.getAbsolutePath()});
                break;
            default:
                throw new IllegalStateException("Invalid config type: " + wizardConfig.getType());
        }

        if (!reportFile.exists()
                || JOptionPane.YES_OPTION == MessageDialogs.showYesNoDialog(dialog.getOwner(), existsMessage, "Overwrite?")) {
            dialog.close(FINISHED_EXIT_CODE);
        }
    }

    public void show() {
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
    }

    public int getExitCode() {
        return dialog.getExitCode();
    }

    public void doCancel() {
        dialog.close(CANCELLED_EXIT_CODE);
    }

    /**
     * Helper method for handling the ReportWizard.
     *
     * @param project current project
     * @param context "universal" project context
     */
    public static void showAndProcess(Project project, IDEContext context) {
        final CloverPluginConfig config = ProjectPlugin.getPlugin(project).getConfig();
        String initString = config.getInitString();
        ReportWizard wizard = new ReportWizard(context);

        // display the wizard.
        wizard.show();

        // evaluate exit code.
        if (ReportWizard.FINISHED_EXIT_CODE != wizard.getExitCode()) {
            return;
        }

        // generate report.
        CloverReportConfig reportConfig = wizard.getReportConfiguration();
        reportConfig.setInitString(initString);

        LOG.info("Generating report: " + reportConfig);
        generateReport(project, reportConfig);
    }


    private static void generateReport(Project project, final CloverReportConfig reportConfig) {
        final String title = "Generating " + reportConfig.getFormat().getType() + " Clover report";
        final String description = title + " '" + reportConfig.getTitle() + "' to " + reportConfig.getOutFile();


        new Task.Backgroundable(project, title, false) {

            private CloverException reportGenerationException;

            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(description);
                try {
                    CloverReporter.buildReporter(reportConfig).execute();
                } catch (CloverException e) {
                    reportGenerationException = e;
                }
            }

            /**
             * Called on UI thread when task finishes without cancellation (which is always in this setup) 
             */
            @Override
            public void onSuccess() {
                if (reportGenerationException == null) {
                    MessageDialogs.showInfoMessage(null, "Your Clover report has been generated and " +
                            "written to " + reportConfig.getOutFile().getAbsolutePath(),
                            "Generation confirmation.");
                } else {
                    LOG.info(reportGenerationException);
                    MessageDialogs.showErrorMessage(null, "The following error has occured while " +
                            "generating the Clover report:\n" + reportGenerationException.getMessage(),
                            "Error generating report.");
                }

            }
        }.queue();
    }
}