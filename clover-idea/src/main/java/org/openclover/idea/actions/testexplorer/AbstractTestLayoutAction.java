package org.openclover.idea.actions.testexplorer;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.config.TestCaseLayout;

public abstract class AbstractTestLayoutAction extends ToggleAction {

    protected abstract TestCaseLayout getActionType();

    @Override
    public boolean isSelected(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        return projectPlugin != null && projectPlugin.getConfig().getTestCaseLayout() == getActionType();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean b) {
        if (!b) {
            return;
        }
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            IdeaCloverConfig config = projectPlugin.getConfig();
            config.setTestCaseLayout(getActionType());
            config.notifyListeners();
        }
    }
}