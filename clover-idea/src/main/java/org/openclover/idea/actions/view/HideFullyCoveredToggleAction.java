package org.openclover.idea.actions.view;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;

public class HideFullyCoveredToggleAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        return plugin != null && plugin.getConfig().isHideFullyCovered();

    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            final IdeaCloverConfig config = plugin.getConfig();
            config.setHideFullyCovered(state);
            config.notifyListeners();
        }
    }
}
