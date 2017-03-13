package com.atlassian.clover.eclipse.core.views.actions;

import com.atlassian.clover.Logger;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.runtime.Messages;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.osgi.service.prefs.BackingStoreException;

public class ToggleWorkingSetModeActionDelegate extends UntargetedViewActionDelegate {

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        super.selectionChanged(action, selection);
        action.setChecked(CloverPlugin.getInstance().isInWorkingSetMode());
    }

    @Override
    public void run(IAction action) {
        action.setChecked(!CloverPlugin.getInstance().isInWorkingSetMode());
        final boolean actionChecked = action.isChecked();
        try {
            CloverPlugin.getInstance().setIsInWorkingSetMode(actionChecked);
        } catch (BackingStoreException exception) {
            Logger.getInstance().error(String.format("Could not write WorkingSetMode flag to %b", actionChecked));
            final Status status = new Status(Status.ERROR, CloverPlugin.ID, Status.ERROR, Messages.preferences_saveProblems, exception);
            InternalPlatform.getDefault().log(status);
        }
        CloverProject.refreshAllModels(true, false);
    }
}
