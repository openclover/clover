package org.openclover.eclipse.core.views.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.eclipse.core.views.widgets.context.ContextChooserDialog;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class ShowContextFilterDialogActionDelegate extends SingleCloverProjectActionDelegate {

    @Override
    public void run(IAction action) {

        CloverProject project = null;
        try {
            project = CloverProject.getFor(projects.iterator().next());
        } catch (CoreException e) {
            logError("Unable to retrieve block filter", e);
        }

        if (project != null) {
            final ProjectSettings props = project.getSettings();

            final ContextChooserDialog dialog =
                new ContextChooserDialog(getShell(), props);

            final CloverProject projectAsFinal = project;
            Display.getDefault().asyncExec(() -> {
                int result = dialog.open();

                switch (result) {
                    case IDialogConstants.OK_ID:
                        projectAsFinal.refreshModel(true, false);
                        break;
                    case IDialogConstants.CANCEL_ID:
                        break;
                    default:
                        logError("Unknown dialog response code when setting block contexts: " + result);
                }
            });
        }
    }
}
