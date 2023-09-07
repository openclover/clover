package com.atlassian.clover.idea.actions.cloudreport;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;

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
