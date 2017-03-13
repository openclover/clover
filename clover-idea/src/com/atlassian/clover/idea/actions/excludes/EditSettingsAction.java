package com.atlassian.clover.idea.actions.excludes;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.config.CloverProjectConfigurable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.ProjectConfigPanel;

public class EditSettingsAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            plugin.getConfig().setLastProjectConfigTabSelected(ProjectConfigPanel.COMPILATION_TAB_INDEX);
            ShowSettingsUtil.getInstance().showSettingsDialog(e.getData(DataKeys.PROJECT), CloverProjectConfigurable.DISPLAY_NAME);
        }
    }
}
