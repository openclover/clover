package org.openclover.idea.actions.cloudreport;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;

public class IncludeSubpackagesAction extends CheckboxAction {
    @Override
    public boolean isSelected(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        return plugin != null && plugin.getConfig().isCloudReportIncludeSubpkgs();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(ProjectPlugin.getPlugin(e) != null);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            plugin.getConfig().setCloudReportIncludeSubpkgs(state);
            plugin.getConfig().notifyListeners();
        }
    }
}
