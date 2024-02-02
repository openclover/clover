package org.openclover.idea.actions.testexplorer;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.util.ui.CloverIcons;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

public class LoadPerTestDataToggleAction extends ToggleAction {
    public LoadPerTestDataToggleAction() {
        getTemplatePresentation().setIcon(CloverIcons.ALWAYS_LOAD_PER_TEST_COVERAGE);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        return plugin != null && plugin.getConfig().isLoadPerTestData();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            final IdeaCloverConfig config = plugin.getConfig();
            config.setLoadPerTestData(state);
            config.notifyListeners();
        }

    }
}
