package org.openclover.eclipse.core.views.nodes;

import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.CloverPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;

public class ProjToPkgFragRootRelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            CloverProject cloverProject = CloverProject.getFor((IProject) object);
            if (cloverProject != null && cloverProject.getModel().isLoaded()) {
                IPackageFragmentRoot[] roots =
                    cloverProject.getJavaProject().getPackageFragmentRoots();
                Collection<Object> sourceRoots = new ArrayList<Object>(roots.length);

                final NodeRelationship pkgFragRootToPkgFrag = new PkgFragRootToPkgFragRelationship();
                for (IPackageFragmentRoot root : roots) {
                    if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        //For projects where root is top level project, just add the packages directly.
                        //In that case there'll only be one root anyway
                        if (root.getPath() == root.getJavaProject().getPath()) {
                            sourceRoots.addAll(
                                    Arrays.asList(pkgFragRootToPkgFrag.getChildren(root, filter)));
                            break;
                        } else {
                            if (pkgFragRootToPkgFrag.hasChildren(root, filter)) {
                                sourceRoots.add(root);
                            }
                        }
                    }
                }
                return filter.perform(sourceRoots);
            }
        } catch (Exception e) {
            CloverPlugin.logError("Unable to retrieve children for project " + object, e);
        }
        return new Object[]{};
    }

    @Override
    public Boolean hasChildren(Object object, NodeRelationshipFilter filter) {
        try {
            CloverProject cloverProject = CloverProject.getFor((IProject) object);
            if (cloverProject != null && cloverProject.getModel().isLoaded()) {
                IPackageFragmentRoot[] roots =
                    cloverProject.getJavaProject().getPackageFragmentRoots();

                final NodeRelationship pkgFragRootToPkgFrag = new PkgFragRootToPkgFragRelationship();
                for (IPackageFragmentRoot root : roots) {
                    if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        //For projects where root is top level project, just add the packages directly.
                        //In that case there'll only be one root anyway
                        if (root.getPath() == root.getJavaProject().getPath()) {
                            return pkgFragRootToPkgFrag.hasChildren(root, filter);
                        } else {
                            if (pkgFragRootToPkgFrag.hasChildren(root, filter) && filter.accept(root)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CloverPlugin.logError("Unable to retrieve children for project " + object, e);
        }
        return false;
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof IProject;
    }
}
