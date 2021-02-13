package com.atlassian.clover.eclipse.core.views.actions;

import com.atlassian.clover.eclipse.core.projects.settings.ProjectPropertyPage;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ConfigureCloverActionDelegate
    extends CloverProjectActionDelegate {

    @Override
    public void run(IAction action) {
        for (IProject project : projects) {
            PreferencesUtil.createPropertyDialogOn(
                    part.getSite().getShell(),
                    project,
                    ProjectPropertyPage.ID, null, null).open();
        }
    }
}
