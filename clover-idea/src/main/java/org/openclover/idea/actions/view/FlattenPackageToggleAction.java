package org.openclover.idea.actions.view;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import org.openclover.idea.actions.CloverToggleAction;
import com.intellij.openapi.project.Project;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;

public class FlattenPackageToggleAction extends CloverToggleAction {

    @Override
    public boolean isSelected(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return false;
        }
        IdeaCloverConfig projectConfig = ProjectPlugin.getPlugin(project).getConfig();
        return projectConfig.isFlattenPackages();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            IdeaCloverConfig projectConfig = ProjectPlugin.getPlugin(project).getConfig();
            projectConfig.setFlattenPackages(b);
            projectConfig.notifyListeners();
        }
    }
}
