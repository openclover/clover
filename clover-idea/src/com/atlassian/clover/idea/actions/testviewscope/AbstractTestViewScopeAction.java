package com.atlassian.clover.idea.actions.testviewscope;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.config.TestViewScope;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public abstract class AbstractTestViewScopeAction extends AnAction {
    protected abstract TestViewScope getActionType();

    public boolean isSelected(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        return projectPlugin != null && projectPlugin.getConfig().getTestViewScope() == getActionType();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            IdeaCloverConfig config = projectPlugin.getConfig();
            config.setTestViewScope(getActionType());
            config.notifyListeners();
        }
    }
}
