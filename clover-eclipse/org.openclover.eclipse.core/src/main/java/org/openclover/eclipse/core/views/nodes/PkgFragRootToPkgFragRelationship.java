package com.atlassian.clover.eclipse.core.views.nodes;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import java.util.List;
import java.util.ArrayList;

public class PkgFragRootToPkgFragRelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            IPackageFragmentRoot root = (IPackageFragmentRoot) object;
            IJavaElement[] children = root.getChildren();
            List<IPackageFragment> packageFragments = new ArrayList<IPackageFragment>(children.length);
            for (IJavaElement child : children) {
                if (child instanceof IPackageFragment
                        && ((IPackageFragment) child).getCompilationUnits().length > 0) {
                    packageFragments.add((IPackageFragment)child);
                }
            }
            return filter.perform(packageFragments);
        } catch (Exception e) {
            CloverPlugin.logError("Unable to retrieve children for package fragment " + object, e);
        }
        return new Object[]{};
    }

    @Override
    public Boolean hasChildren(Object object, NodeRelationshipFilter filter) {
        try {
            IPackageFragmentRoot root = (IPackageFragmentRoot) object;
            IJavaElement[] children = root.getChildren();
            for (IJavaElement child : children) {
                if (child instanceof IPackageFragment
                        && ((IPackageFragment) child).getCompilationUnits().length > 0) {
                    if (filter.accept(child)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            CloverPlugin.logError("Unable to retrieve children for package fragment " + object, e);
        }
        return false;
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof IPackageFragmentRoot;
    }
}
