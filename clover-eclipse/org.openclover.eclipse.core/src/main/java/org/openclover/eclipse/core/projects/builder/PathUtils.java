package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.openclover.eclipse.core.CloverPlugin;

public class PathUtils {
    public static boolean isAncestorOrDescendantOfContainer(IContainer container, IResource resource) {
        return isDescendantOfContainer(container, resource) || isAncestorOfContainer(container, resource);
    }

    public static boolean isAncestorOfContainer(IContainer container, IResource resource) {
        return resource.getFullPath().isPrefixOf(container.getFullPath());
    }

    public static boolean isAncestorOfOrSameContainer(IContainer container, IResource resource) {
        return container.equals(resource) || resource.getFullPath().isPrefixOf(container.getFullPath());
    }

    public static boolean isDescendantOfContainer(IContainer container, IResource resource) {
        return container.getFullPath().isPrefixOf(resource.getFullPath());
    }

    public static IResource makeDerivedFoldersFor(IResource resource) throws CoreException {
        IContainer parent = resource.getParent();
        if (parent instanceof IFolder) {
            makeDerivedFoldersFor(parent);
        }

        if (resource instanceof IFolder) {
            try {
                resource.refreshLocal(IResource.DEPTH_ZERO, null);
            } catch (CoreException e) {
                CloverPlugin.logDebug("Unable to refresh folder to see if it exists or not: " + resource.getLocation(), e);
            }
            if (!resource.exists()) {
                makeDerivedFolder((IFolder)resource);
            }
        }
        return resource;
    }

    public static IResource makeFoldersFor(IResource resource) throws CoreException {
        IContainer parent = resource.getParent();
        if (parent instanceof IFolder) {
            makeFoldersFor(parent);
        }

        if (resource instanceof IFolder && !resource.exists()) {
            makeFolder((IFolder)resource);
        }
        return resource;
    }

    public static void makeFolder(IFolder folder) throws CoreException {
        folder.create(IResource.DERIVED, true, null);
    }

    public static void makeDerivedFolder(IFolder folder) throws CoreException {
        folder.create(IResource.FORCE | IResource.DERIVED, true, null);
    }

    public static IContainer containerFor(IPath path) {
        if (ResourcesPlugin.getWorkspace().validatePath(path.toString(), IResource.FOLDER).getCode() == IStatus.OK) {
            return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
        } else if (ResourcesPlugin.getWorkspace().validatePath(path.toString(), IResource.PROJECT).getCode() == IStatus.OK) {
            return ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
        } else {
            return null;
        }
    }
}
