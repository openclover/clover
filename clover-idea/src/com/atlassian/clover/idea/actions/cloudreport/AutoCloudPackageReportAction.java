package com.atlassian.clover.idea.actions.cloudreport;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

public class AutoCloudPackageReportAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent event) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(event);
        return plugin != null && plugin.getConfig().isAutoViewInCloudReport();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(event);
        if (plugin != null) {
            final IdeaCloverConfig projectConfig = plugin.getConfig();
            projectConfig.setAutoViewInCloudReport(b);
            projectConfig.notifyListeners();
        }
    }
}
