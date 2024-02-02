package org.openclover.idea.actions;

import org.openclover.idea.AboutDialog;
import org.openclover.idea.util.ui.CloverIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;

import javax.swing.*;

public class AboutAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        new AboutDialog(DataKeys.PROJECT.getData(e.getDataContext())).show();
    }

    @Override
    public void update(AnActionEvent e) {
        final Icon icon = CloverIcons.ABOUT;
        final Presentation presentation = e.getPresentation();
        if (presentation.getIcon() != icon) {
            presentation.setIcon(icon);
        }

        final String actionText = getTemplatePresentation().getText();
        presentation.setText(actionText);
    }
}
