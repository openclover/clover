package org.openclover.idea.actions.cloudreport;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
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
