package com.atlassian.clover.eclipse.testopt.actions;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import java.util.Iterator;

public class ActionUtils {
    private ActionUtils() {
    }

    public static boolean isInCloverNature(ISelection iSelection) {
        if (iSelection instanceof IStructuredSelection) {
            final Iterator selection = ((IStructuredSelection) iSelection).iterator();
            while (selection.hasNext()) {
                final Object o = selection.next();
                if (o instanceof IAdaptable) {
                    IResource resource = (IResource) ((IAdaptable) o).getAdapter(IResource.class);
                    try {
                        if (resource != null && CloverProject.isAppliedTo(resource.getProject())) {
                            return true;
                        }
                    } catch (CoreException e) {
                        CloverPlugin.logWarning("Unable to determine project nature", e);
                    }
                }
            }
        }

        return false;
    }
}
