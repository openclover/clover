package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.util.ui.CloverIcons;

import javax.swing.Icon;

public class ChooseModelScopeAction extends DefaultActionGroup {


    @Override
    public void update(AnActionEvent event) {
        super.update(event);

        Icon icon = CloverIcons.MODEL_SCOPE_ALL;

        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);

        if (projectPlugin != null) {
            switch (projectPlugin.getConfig().getModelScope()) {
                case TEST_CLASSES_ONLY:
                    icon = CloverIcons.MODEL_SCOPE_TEST;
                    break;
                case APP_CLASSES_ONLY:
                    icon = CloverIcons.MODEL_SCOPE_APP;
                    break;
            }
        }

        event.getPresentation().setIcon(icon);


    }
}
