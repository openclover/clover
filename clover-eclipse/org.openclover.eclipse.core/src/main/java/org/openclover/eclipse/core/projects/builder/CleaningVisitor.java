package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class CleaningVisitor implements IResourceVisitor {
    private IFolder parent;
    private IProgressMonitor monitor;

    public CleaningVisitor(IFolder parent, IProgressMonitor monitor) {
        super();
        this.parent = parent;
        this.monitor = monitor;
    }

    @Override
    public boolean visit(IResource resource) throws CoreException {
        if (PathUtils.isAncestorOfOrSameContainer(parent, resource)) {
            return true;
        } else if (PathUtils.isDescendantOfContainer(parent, resource)) {
            monitor.subTask("Removing " + resource.getFullPath().toOSString());
            resource.delete(IResource.FORCE, null);
            return true;
        }
        return false;
    }
}