package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.NodeRelationshipFilter;
import org.openclover.eclipse.core.views.nodes.PackageFragmentNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Sets.newLinkedHashSet;

public class PkgFragToTypeRelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            List<IPackageFragment> packageFragments = toPackageFragments(object);
            Set<IType> compilationUnits = newLinkedHashSet();

            for (IPackageFragment packageFragment : packageFragments) {
                if (packageFragment.exists()) {
                    compilationUnits.addAll(collectTypes(packageFragment.getCompilationUnits()));
                }
            }

            return filter.perform(compilationUnits);
        } catch (Exception e) {
            CloverPlugin.logError("Unable to meta-collect compilation units of package fragment " + object, e);
            return new Object[] {};
        }
    }

    private Collection<IType> collectTypes(ICompilationUnit[] compilationUnits) throws JavaModelException {
        List<IType> types = newLinkedList();
        for (ICompilationUnit compilationUnit : compilationUnits) {
            types.addAll(Arrays.asList(compilationUnit.getAllTypes()));
        }
        return types;
    }

    private List<IPackageFragment> toPackageFragments(Object object) {
        return
            (object instanceof PackageFragmentNode)
                ? newLinkedList(((PackageFragmentNode)object).getPackageFragments())
                : Collections.singletonList((IPackageFragment)object);
    }

    @Override
    public boolean includes(Object object) {
        return
            object instanceof IPackageFragment
            || object instanceof PackageFragmentNode;
    }
}
