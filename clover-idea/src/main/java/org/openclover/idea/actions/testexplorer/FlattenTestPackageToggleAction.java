package com.atlassian.clover.idea.actions.testexplorer;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.actions.view.FlattenPackageToggleAction;
import com.atlassian.clover.idea.config.TestCaseLayout;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;

public class FlattenTestPackageToggleAction extends FlattenPackageToggleAction {
    private final String disabledToolTipText;

    public FlattenTestPackageToggleAction() {
        String listLayoutName = ActionManager.getInstance().getAction("CloverPlugin.TestExplorer.TestCaseLayout").getTemplatePresentation().getText();
        disabledToolTipText = "Setting unavailable when Test Cases Layout is set to " + listLayoutName;
    }

    @Override
    public void update(AnActionEvent event) {
        super.update(event);

        final Presentation presentation = event.getPresentation();

        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        if (projectPlugin != null && projectPlugin.getConfig().getTestCaseLayout() != TestCaseLayout.TEST_CASES) {
            presentation.setEnabled(true);
            presentation.setText(getTemplatePresentation().getText());
        } else {
            presentation.setEnabled(false);
            presentation.setText(disabledToolTipText);
        }

    }
}
