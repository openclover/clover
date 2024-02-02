package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.util.ModelScope;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;

import javax.swing.Icon;

public abstract class AbstractClassesScopeAction extends AnAction {

    protected abstract ModelScope getActionType();

    public boolean isSelected(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        return projectPlugin != null && projectPlugin.getConfig().getModelScope() == getActionType();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null) {
            IdeaCloverConfig config = projectPlugin.getConfig();
            config.setModelScope(getActionType());
            config.notifyListeners();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final ModelScope actionType = getActionType();

        presentation.setText(getText(actionType));
        presentation.setIcon(getIcon(actionType));
    }

    public static Icon getIcon(ModelScope scope) {
        switch (scope) {
            case APP_CLASSES_ONLY:
                return CloverIcons.SOURCE_ROOT_FOLDER;
            case TEST_CLASSES_ONLY:
                return CloverIcons.TEST_ROOT_FOLDER;
            default:
                return CloverIcons.FOLDER;
        }
    }

    public static String getText(ModelScope scope) {
        switch (scope) {
            case APP_CLASSES_ONLY:
                return "App. Classes";
            case TEST_CLASSES_ONLY:
                return "Test Classes";
            default:
                return "All Classes";
        }

    }

}
