package org.openclover.eclipse.core.views.nodes;

import org.openclover.eclipse.core.CloverPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Sets.newLinkedHashSet;

/**
 * Collects the children of IPackageFragments nodes as ICompliationUnits nodes
*/
public class PkgFragToCURelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            List<IPackageFragment> packageFragments = toPackageFragments(object);
            Set<ICompilationUnit> compilationUnits = newLinkedHashSet();
            for (IPackageFragment packageFragment : packageFragments) {
                if (packageFragment.exists()) {
                    compilationUnits.addAll(Arrays.asList(packageFragment.getCompilationUnits()));
                }
            }

            return filter.perform(compilationUnits);
        } catch (Exception e) {
            CloverPlugin.logError("Unable to meta-collect compilation units of package fragment " + object, e);
            return new Object[] {};
        }
    }

    @Override
    public Boolean hasChildren(Object object, NodeRelationshipFilter filter) {
        try {
            List<IPackageFragment> packageFragments = toPackageFragments(object);
            for (IPackageFragment packageFragment : packageFragments) {
                if (packageFragment.exists() && filter.accept(packageFragment)) {
                    return true;
                }
            }
        } catch (Exception e) {
            CloverPlugin.logError("Unable to meta-collect compilation units of package fragment " + object, e);
        }
        return false;
    }

    private List<IPackageFragment> toPackageFragments(Object object) {
        return
            (object instanceof PackageFragmentNode)
                ? newLinkedList(((PackageFragmentNode)object).getPackageFragments())
                : Collections.singletonList((IPackageFragment) object);
    }

    @Override
    public boolean includes(Object object) {
        return
            object instanceof IPackageFragment
            || object instanceof PackageFragmentNode;
    }
}
