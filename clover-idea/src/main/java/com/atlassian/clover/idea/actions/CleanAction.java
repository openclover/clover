package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.CloverToolWindowId;
import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindowManager;

public class CleanAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);
            if (!projectPlugin.getCoverageManager().clean()) {
                ToolWindowManager.getInstance(project).notifyByBalloon(
                        CloverToolWindowId.TOOL_WINDOW_ID,
                        MessageType.WARNING,
                        "Clover was unable to clean the coverage data. <br/>"
                        + "See the IDEA log for more details.");
            }
        }
    }
}
