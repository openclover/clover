package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            projectPlugin.getCoverageManager().reload();
        }
    }
}
