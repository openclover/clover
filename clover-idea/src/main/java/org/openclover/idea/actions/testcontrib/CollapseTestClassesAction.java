package org.openclover.idea.actions.testcontrib;

import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

public class CollapseTestClassesAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return false;
        }
        return ProjectPlugin.getPlugin(project).getConfig().isAlwaysCollapseTestClasses();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            final IdeaCloverConfig cloverConfig = ProjectPlugin.getPlugin(project).getConfig();
            cloverConfig.setAlwaysCollapseTestClasses(b);
            cloverConfig.notifyListeners();
        }
    }
}
