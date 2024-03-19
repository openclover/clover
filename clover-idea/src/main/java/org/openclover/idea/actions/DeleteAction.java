package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindowManager;
import org.openclover.idea.CloverToolWindowId;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.build.ProjectRebuilder;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.feature.CloverFeatures;

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
                        "OpenClover was unable to delete the coverage database. <br/>"
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
