package org.openclover.idea.config;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;

public class AddFilterAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        // create a new filter using default configurations.
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            IdeaRegexpConfigPanel panel = IdeaRegexpConfigPanel.getInstance(project);
            if (panel != null) {
                panel.doAdd();
            }
        }
    }

    @Override
    public void update(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            IdeaRegexpConfigPanel panel = IdeaRegexpConfigPanel.getInstance(project);
            event.getPresentation().setEnabled(panel != null && panel.isEnabled());
        } else {
            event.getPresentation().setEnabled(false);
        }
    }
}