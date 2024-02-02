package org.openclover.idea.actions.testexplorer;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.util.ui.CloverIcons;
import org.openclover.idea.ProjectPlugin;
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
