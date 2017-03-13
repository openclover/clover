package com.atlassian.clover.idea.actions.view;

import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

public class AutoScrollFromSourceToggleAction extends ToggleAction {

    @Override
    public boolean isSelected(AnActionEvent event) {
        Project project = (Project) DataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return false;
        }
        IdeaCloverConfig projectConfig = ProjectPlugin.getPlugin(project).getConfig();
        return projectConfig.isAutoScrollFromSource();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        Project project = (Project) DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            IdeaCloverConfig projectConfig = ProjectPlugin.getPlugin(project).getConfig();
            projectConfig.setAutoScrollFromSource(b);
            projectConfig.notifyListeners();
        }
    }
}
