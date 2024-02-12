package org.openclover.idea.actions.excludes;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.CloverProjectConfigurable;
import org.openclover.idea.config.ProjectConfigPanel;

public class EditSettingsAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            plugin.getConfig().setLastProjectConfigTabSelected(ProjectConfigPanel.COMPILATION_TAB_INDEX);
            ShowSettingsUtil.getInstance().showSettingsDialog(e.getData(DataKeys.PROJECT), CloverProjectConfigurable.DISPLAY_NAME);
        }
    }
}
