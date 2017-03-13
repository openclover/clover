package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.feature.CloverFeatures;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

public class ViewAllCoverageAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return false;
        }

        final IdeaCloverConfig cloverConfig = ProjectPlugin.getPlugin(project).getConfig();
        return cloverConfig.isShowInline() && cloverConfig.isHighlightCovered();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        if (b) {
            Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project != null) {
                final IdeaCloverConfig cloverConfig = ProjectPlugin.getPlugin(project).getConfig();
                cloverConfig.setHighlightCovered(true);
                cloverConfig.setShowInline(true);
                cloverConfig.notifyListeners();
            }
        }
    }

    @Override
    public void update(AnActionEvent event) {
        super.update(event);

        boolean enabled = false;
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            enabled = ProjectPlugin.getPlugin(project).getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_REPORTING);
        }
        event.getPresentation().setVisible(enabled);
    }
}