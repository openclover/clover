package org.openclover.eclipse.core.views.nodes;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;

import org.openclover.eclipse.core.CloverPlugin;

import static clover.com.google.common.collect.Lists.newLinkedList;
import static clover.com.google.common.collect.Maps.newHashMap;
import static clover.com.google.common.collect.Sets.newHashSet;

public class PhysicalTreePkgFragNodeToTreeMultiPkgFragNodeAndCURelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            final List<Object/*TreePackageFragmentNode or ICompilationUnit*/> children = newLinkedList();
            final List<ICompilationUnit> CUs = newLinkedList();

            final TreePackageFragmentNode pkgFragNode = (TreePackageFragmentNode)object;
            final String pkgName = pkgFragNode.getElementName();
            final IJavaProject javaProject = pkgFragNode.toJavaElement().getJavaProject();

            final Map<String, Set<IPackageFragment>> pkgNamesToFragments = newHashMap();

            for (IPackageFragmentRoot root : Arrays.asList(javaProject.getPackageFragmentRoots())) {
                if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    final IPackageFragment pkgFragmentForRoot = root.getPackageFragment(pkgName);
                    if (pkgFragmentForRoot != null) {
                        if (pkgFragmentForRoot.exists()) {
                            CUs.addAll(Arrays.asList(pkgFragmentForRoot.getCompilationUnits()));
                        }

                        final int subPkgCount = pkgName.split("\\.").length;
                        final IJavaElement[] siblingPkgs = root.getChildren();
                        for (IJavaElement siblingPkg : siblingPkgs) {
                            if (siblingPkg instanceof IPackageFragment
                                    && siblingPkg.getElementName().indexOf(pkgFragmentForRoot.getElementName() + ".") == 0
                                    && siblingPkg.getElementName().split("\\.").length == subPkgCount + 1) {

                                Set<IPackageFragment> pkgFragmentsForName = pkgNamesToFragments.get(siblingPkg.getElementName());
                                if (pkgFragmentsForName == null) {
                                    pkgFragmentsForName = newHashSet();
                                    pkgNamesToFragments.put(siblingPkg.getElementName(), pkgFragmentsForName);
                                }
                                pkgFragmentsForName.add(new PackageFragmentAdapter((IPackageFragment) siblingPkg) {
                                    @Override
                                    public String getElementName() {
                                        return
                                                super.getElementName().substring(pkgName.length() + 1);
                                    }
                                });
                            }
                        }
                    }

                }
            }

            for (Map.Entry<String, Set<IPackageFragment>> entry : pkgNamesToFragments.entrySet()) {
                children.add(
                        new TreePackageFragmentNode(
                                entry.getKey(),
                                entry.getValue()));
            }
            children.addAll(CUs);
            
            return filter.perform(children);
        } catch (Exception e) {
            CloverPlugin.logError("Unable to meta-collect compilation units of package fragment " + object, e);
            return new Object[] {};
        }
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof PackageFragmentNode;
    }
}
