package org.openclover.idea.actions;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
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
