package org.openclover.eclipse.core.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.ui.projects.widgets.ToggleCloverProjectsDialog;

import java.util.Set;

public class ToggleMultipleCloverProjectNaturesActionDelegate
    extends CloverViewActionDelegate
    implements IObjectActionDelegate {
    private IWorkbenchPart part;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart part) {
        this.part = part;
    }

    public IWorkbenchPage getPage() {
        return part == null
            ? view.getViewSite().getPage()
            : part.getSite().getPage();
    }

    @Override
    public void run(IAction action) {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final ToggleCloverProjectsDialog dialog = new ToggleCloverProjectsDialog(shell);

        Display.getDefault().asyncExec(() -> {
            if (dialog.open() == Dialog.OK) {
                final Set toToggle = dialog.getProjectsToToggle();
                for (Object o : toToggle) {
                    CloverProject.toggleWithUserFeedback(
                            shell,
                            (IProject) o);
                }
                try {
                    // Automatically show Clover views if at least one project will have Clover enabled
                    // and "Auto open clover views" toggle in Preferences in enabled
                    if ( CloverPlugin.getInstance().getInstallationSettings().isAutoOpenCloverViews()
                            && (toToggle.size() > 0) ) {
                        CloverPlugin.getInstance().showViews(getPage());
                    }
                } catch (CoreException e) {
                    CloverPlugin.logError("Unable to show views after toggling Clover on " + toToggle.size() + " projects", e);
                }
            }
        });
    }
}
