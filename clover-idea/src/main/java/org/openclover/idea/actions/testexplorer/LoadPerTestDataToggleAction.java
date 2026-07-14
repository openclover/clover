package org.openclover.idea.actions.testexplorer;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.openclover.idea.actions.CloverToggleAction;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.util.ui.CloverIcons;

public class LoadPerTestDataToggleAction extends CloverToggleAction {
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
