package com.atlassian.clover.idea.actions.testexplorer;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.ProjectPlugin;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

import javax.swing.Icon;

public class ChooseTestCaseLayoutAction extends DefaultActionGroup {
    @Override
    public void update(AnActionEvent event) {
        super.update(event);


        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        event.getPresentation().setVisible(projectPlugin != null);
        if (projectPlugin != null) {
            final Icon icon;
            switch (projectPlugin.getConfig().getTestCaseLayout()) {
                case PACKAGES:
                    icon = CloverIcons.TEST_PACKAGE;
                    break;
                case SOURCE_ROOTS:
                    icon = CloverIcons.TEST_ROOT_FOLDER;
                    break;
                default:
                    icon = CloverIcons.TEST_METHOD;
            }
            event.getPresentation().setIcon(icon);
        }


    }

}
