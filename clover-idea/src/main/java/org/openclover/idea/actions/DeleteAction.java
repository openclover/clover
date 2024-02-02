package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.CloverToolWindowId;
import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.coverage.CoverageManager;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.build.ProjectRebuilder;
import com.atlassian.clover.idea.feature.CloverFeatures;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindowManager;

import java.io.File;
import java.net.URISyntaxException;

public class DeleteAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);
            if (!projectPlugin.getCoverageManager().delete()) {
                ToolWindowManager.getInstance(project).notifyByBalloon(
                        CloverToolWindowId.TOOL_WINDOW_ID,
                        MessageType.WARNING,
                        "Clover was unable to delete the coverage database. <br/>"
                        + "See the IDEA log for more details.");
            }
            if (projectPlugin.getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_BUILDING)) {
                ProjectRebuilder.getInstance(project).rebuildProject();
            }
        }
    }

    @Override
    public void update(AnActionEvent event) {
        boolean enabled = false;
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            CoverageManager manager = projectPlugin.getCoverageManager();
            if (manager.getCoverage() != null) {
                enabled = true;
            } else {
                if (manager.getInitString() != null) {
                    try {
                        File f = new File(manager.getInitString().toURI());
                        enabled = f.exists();
                    } catch (URISyntaxException e) {
                        enabled = false;
                    }
                }
            }
        }
        event.getPresentation().setEnabled(enabled);
    }
}
