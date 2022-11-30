package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;

public abstract class AbstractReportAction extends AnAction {
    @Override
    public void update(AnActionEvent event) {
        super.update(event);

        final IProjectPlugin plugin = ProjectPlugin.getPlugin(event);
        final boolean enabled = plugin != null && plugin.getCoverageManager().getCoverage() != null;

        final Presentation presentation = event.getPresentation();
        presentation.setEnabled(enabled);
        if (enabled) {
            presentation.setText(getTemplatePresentation().getText());
        } else {
            presentation.setText(getTemplatePresentation().getText() + " unavailable: refresh coverage data.");
        }
    }
}
