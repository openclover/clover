package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.CloverProjectConfigurable;
import com.atlassian.clover.idea.config.ProjectConfigPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

public class ShowFilterSettingsAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());

        if (project != null) {
            final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);
            projectPlugin.getConfig().setLastProjectConfigTabSelected(ProjectConfigPanel.FILTER_TAB_INDEX);
            ShowSettingsUtil.getInstance().showSettingsDialog(project, CloverProjectConfigurable.DISPLAY_NAME);
        }
    }
}
