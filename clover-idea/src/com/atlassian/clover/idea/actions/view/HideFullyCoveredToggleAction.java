package com.atlassian.clover.idea.actions.view;

import com.atlassian.clover.idea.IProjectPlugin;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;

public class HideFullyCoveredToggleAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        return plugin != null && plugin.getConfig().isHideFullyCovered();

    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            final IdeaCloverConfig config = plugin.getConfig();
            config.setHideFullyCovered(state);
            config.notifyListeners();
        }
    }
}
