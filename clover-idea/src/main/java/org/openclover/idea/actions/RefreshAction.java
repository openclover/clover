package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;

public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            projectPlugin.getCoverageManager().reload();
        }
    }
}
