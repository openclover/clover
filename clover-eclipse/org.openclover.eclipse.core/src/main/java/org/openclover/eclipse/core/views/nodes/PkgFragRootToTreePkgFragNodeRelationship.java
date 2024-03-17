package org.openclover.eclipse.core.views.nodes;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.openclover.eclipse.core.CloverPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class PkgFragRootToTreePkgFragNodeRelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            IPackageFragmentRoot root = (IPackageFragmentRoot) object;
            IJavaElement[] children = root.getChildren();
            List<TreePackageFragmentNode> packageFragments = new ArrayList<>(children.length);
            for (IJavaElement child : children) {
                if (child instanceof IPackageFragment
                        && isTopLevelPackage((IPackageFragment) child)) {
                    packageFragments.add(
                            new TreePackageFragmentNode(
                                    child.getElementName(),
                                    Collections.singleton((IPackageFragment) child)));
                }
            }
            return filter.perform(packageFragments);
        } catch (Exception e) {
            logError("Unable to retrieve children for package fragment " + object, e);
        }
        return new Object[]{};
    }

    private boolean isTopLevelPackage(IPackageFragment packageFragment) throws JavaModelException {
        if (packageFragment.isDefaultPackage()) {
            return packageFragment.getCompilationUnits().length > 0;
        } else {
            return
                    !packageFragment.getElementName().contains(".")
                && ((packageFragment.hasSubpackages()
                    || packageFragment.getCompilationUnits().length > 0));
        }
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof IPackageFragmentRoot;
    }
}
