package org.openclover.eclipse.testopt.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IStartup;
import org.openclover.core.optimization.Snapshot;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.views.actions.MultiCloverProjectActionDelegate;

import java.io.File;

public class ClearSnapshotActionDelegate extends MultiCloverProjectActionDelegate implements IStartup {
    @Override
    protected boolean enableFor(IProject project) throws CoreException {
        final File snapshot = snapshotFor(project);
        return snapshot != null && snapshot.exists();
    }

    private static File snapshotFor(IProject project) throws CoreException {
        final CloverProject cloverProject = CloverProject.getFor(project);
        return cloverProject != null ? Snapshot.fileForInitString(cloverProject.getRegistryFile().getAbsolutePath()) : null;
    }

    @Override
    public void run(IAction action) {
        try {
            for (IProject project : projects) {
                final File snapshot = snapshotFor(project);
                if (snapshot != null && snapshot.exists()) {
                    snapshot.delete();
                }
            }
        } catch (Exception e) {
            CloverPlugin.logError("Unable to remove snapshot file", e);
        }
        updateStateForSelection(action);
    }

    @Override
    public void earlyStartup() {
    }
}
