package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.IdeaIDEContext;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.report.ReportWizard;
import com.atlassian.clover.idea.util.l10n.CloverIdeaPluginMessages;
import com.atlassian.clover.idea.util.ui.HTMLDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;

/**
 * The show report wizard action handles the report wizard integration into
 * IDEA.
 */
public class ShowReportWizardAction extends AnAction {

    /**
     * The update method manages the 'enabled' state of this action.
     * <p>This method receives a callback every couple seconds.
     */
    @Override
    public void update(AnActionEvent event) {
        super.update(event);

        final IProjectPlugin plugin = ProjectPlugin.getPlugin(event);
        event.getPresentation().setEnabled(plugin != null && plugin.getCoverageManager().getCoverage() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        if (isReportingAllowed()) {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());

            // initialise the wizard.
            final IdeaIDEContext context = new IdeaIDEContext(project);

            ReportWizard.showAndProcess(project, context);
        } else {
            new HTMLDialog(CloverIdeaPluginMessages.getString("Report.notLicensedTitle"), CloverIdeaPluginMessages.getString("Report.notLicensed")).show();
        }

    }

    private boolean isReportingAllowed() {
        return true;
    }
}