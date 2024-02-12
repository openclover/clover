package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.IdeaIDEContext;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.report.ReportWizard;

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
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());

        // initialise the wizard.
        final IdeaIDEContext context = new IdeaIDEContext(project);
        ReportWizard.showAndProcess(project, context);
    }

}