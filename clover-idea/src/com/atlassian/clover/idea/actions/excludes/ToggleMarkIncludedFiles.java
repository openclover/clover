package com.atlassian.clover.idea.actions.excludes;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

public class ToggleMarkIncludedFiles extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        return plugin != null && plugin.getConfig().isViewIncludeAnnotation();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            final IdeaCloverConfig config = plugin.getConfig();
            config.setViewIncludeAnnotation(state);
            config.notifyListeners();
        }
    }
}
