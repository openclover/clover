package org.openclover.eclipse.core.views.nodes;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.openclover.eclipse.core.CloverPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.eclipse.core.CloverPlugin.logError;

public class LogicalTreePkgFragNodeToTreeMultiPkgFragNodeAndCURelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            List children = newLinkedList();
            IPackageFragment pkg = (IPackageFragment)((TreePackageFragmentNode)object).toJavaElement();
            pkg =
                pkg instanceof PackageFragmentAdapter
                    ? (IPackageFragment)((PackageFragmentAdapter)pkg).getAdaptee()
                    : pkg;
            final String pkgName = pkg.getElementName();
            final int subPkgCount = pkgName.split("\\.").length;
            if (pkg.exists()) {
                if (!pkg.isDefaultPackage()) {
                    final IJavaElement[] siblingPkgs = ((IPackageFragmentRoot)pkg.getParent()).getChildren();
                    for (IJavaElement siblingPkg : siblingPkgs) {
                        if (siblingPkg instanceof IPackageFragment
                                && siblingPkg != pkg
                                && siblingPkg.getElementName().indexOf(pkg.getElementName()) == 0
                                && siblingPkg.getElementName().split("\\.").length == subPkgCount + 1) {
                            children.add(
                                    new TreePackageFragmentNode(
                                            siblingPkg.getElementName(),
                                            Collections.singleton(
                                                    new PackageFragmentAdapter((IPackageFragment) siblingPkg) {
                                                        @Override
                                                        public String getElementName() {
                                                            return
                                                                    super.getElementName().substring(pkgName.length() + 1);
                                                        }
                                                    })));
                        }
                    }
                }
                children.addAll(Arrays.asList(pkg.getCompilationUnits()));
            }

            return filter.perform(children);
        } catch (Exception e) {
            logError("Unable to meta-collect compilation units of package fragment " + object, e);
            return new Object[] {};
        }
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof TreePackageFragmentNode;
    }
}