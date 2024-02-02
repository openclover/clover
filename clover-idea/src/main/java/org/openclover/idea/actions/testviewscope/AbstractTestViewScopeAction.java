package org.openclover.idea.actions.testviewscope;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.config.TestViewScope;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public abstract class AbstractTestViewScopeAction extends AnAction {
    protected abstract TestViewScope getActionType();

    public boolean isSelected(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        return projectPlugin != null && projectPlugin.getConfig().getTestViewScope() == getActionType();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            IdeaCloverConfig config = projectPlugin.getConfig();
            config.setTestViewScope(getActionType());
            config.notifyListeners();
        }
    }
}
