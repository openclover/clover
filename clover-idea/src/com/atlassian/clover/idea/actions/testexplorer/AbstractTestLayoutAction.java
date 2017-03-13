package com.atlassian.clover.idea.actions.testexplorer;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.config.TestCaseLayout;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

public abstract class AbstractTestLayoutAction extends ToggleAction {

    protected abstract TestCaseLayout getActionType();

    @Override
    public boolean isSelected(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        return projectPlugin != null && projectPlugin.getConfig().getTestCaseLayout() == getActionType();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        if (!b) {
            return;
        }
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            IdeaCloverConfig config = projectPlugin.getConfig();
            config.setTestCaseLayout(getActionType());
            config.notifyListeners();
        }
    }
}