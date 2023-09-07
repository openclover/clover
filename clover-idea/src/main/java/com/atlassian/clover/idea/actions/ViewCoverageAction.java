package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.feature.CloverFeatures;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

public class ViewCoverageAction extends ToggleAction {

    @Override
    public boolean isSelected(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            return ProjectPlugin.getPlugin(project).getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_REPORTING);
        }
        return false;
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            ProjectPlugin.getPlugin(project).getFeatureManager().setCategoryEnabled(CloverFeatures.CLOVER_REPORTING, b);
        }
    }
}
