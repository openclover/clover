package com.atlassian.clover.idea;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;

/**
 * A simple ActionGroup wrapper that becomes visible / invisible depending upon
 * whether or not Clover is enabled for the current project.
 */
public class CloverActionGroupWrapper extends DefaultActionGroup {

    public CloverActionGroupWrapper() {
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);

        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        if (project != null) {
            boolean cloverEnabled = ProjectPlugin.getPlugin(project).isEnabled();
            if (cloverEnabled) {
                e.getPresentation().setVisible(true);
            } else {
                e.getPresentation().setVisible(false);
            }
        } else {
            e.getPresentation().setVisible(false);
        }
    }
}
