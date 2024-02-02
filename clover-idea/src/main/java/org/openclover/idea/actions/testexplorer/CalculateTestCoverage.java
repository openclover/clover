package org.openclover.idea.actions.testexplorer;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

public class CalculateTestCoverage extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        return projectPlugin != null && projectPlugin.getConfig().isCalculateTestCoverage();

    }

    @Override
    public void setSelected(AnActionEvent event, boolean state) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            final IdeaCloverConfig projectConfig = projectPlugin.getConfig();
            projectConfig.setCalculateTestCoverage(state);
            projectConfig.notifyListeners();
        }
    }
}
