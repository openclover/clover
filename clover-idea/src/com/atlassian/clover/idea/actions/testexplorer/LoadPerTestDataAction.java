package com.atlassian.clover.idea.actions.testexplorer;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class LoadPerTestDataAction extends AnAction {
    public LoadPerTestDataAction() {
        getTemplatePresentation().setIcon(CloverIcons.LOAD_COVERAGE_DATA);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin != null) {
            plugin.getCoverageManager().loadCoverageData(true);
        }
    }
}
