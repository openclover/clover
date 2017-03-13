package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

public class IncludePassedTestsCoverageOnlyAction extends ToggleAction {
    /**
     * Returns the selected (checked, pressed) state of the action.
     *
     * @param e the action event representing the place and context in which the selected state is queried.
     * @return true if the action is selected, false otherwise
     */
    @Override
    public boolean isSelected(AnActionEvent e) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(e);
        return projectPlugin != null && projectPlugin.getConfig().isIncludePassedTestCoverageOnly();
    }

    /**
     * Sets the selected state of the action to the specified value.
     *
     * @param e     the action event which caused the state change.
     * @param state the new selected state of the action.
     */
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(e);
        if (projectPlugin != null) {
            final IdeaCloverConfig config = projectPlugin.getConfig();
            config.setIncludePassedTestCoverageOnly(state);
            config.notifyListeners();
            projectPlugin.getCoverageManager().loadCoverageData(false);
        }
    }
}
