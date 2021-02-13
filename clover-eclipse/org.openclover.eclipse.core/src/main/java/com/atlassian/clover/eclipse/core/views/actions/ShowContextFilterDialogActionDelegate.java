package com.atlassian.clover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.core.runtime.CoreException;
import com.atlassian.clover.eclipse.core.views.widgets.context.ContextChooserDialog;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.projects.settings.ProjectSettings;
import com.atlassian.clover.eclipse.core.CloverPlugin;

public class ShowContextFilterDialogActionDelegate extends SingleCloverProjectActionDelegate {

    @Override
    public void run(IAction action) {

        CloverProject project = null;
        try {
            project = CloverProject.getFor(projects.iterator().next());
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to retreive block filter", e);
        }

        if (project != null) {
            final ProjectSettings props = project.getSettings();

            final ContextChooserDialog dialog =
                new ContextChooserDialog(getShell(), props);

            final CloverProject projectAsFinal = project;
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    int result = dialog.open();

                    switch (result) {
                        case IDialogConstants.OK_ID:
                            projectAsFinal.refreshModel(true, false);
                            break;
                        case IDialogConstants.CANCEL_ID:
                            break;
                        default:
                            CloverPlugin.logError("Unknown dialog response code when setting block contexts: " + result);
                    }
                }
            });
        }
    }
}
