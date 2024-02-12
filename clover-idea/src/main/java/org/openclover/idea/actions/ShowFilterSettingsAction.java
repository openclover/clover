package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.CloverProjectConfigurable;
import org.openclover.idea.config.ProjectConfigPanel;

public class ShowFilterSettingsAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());

        if (project != null) {
            final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);
            projectPlugin.getConfig().setLastProjectConfigTabSelected(ProjectConfigPanel.FILTER_TAB_INDEX);
            ShowSettingsUtil.getInstance().showSettingsDialog(project, CloverProjectConfigurable.DISPLAY_NAME);
        }
    }
}
