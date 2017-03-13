package com.atlassian.clover.eclipse.core.views.nodes;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import java.util.Collections;
import java.util.Set;
import java.util.LinkedHashSet;

public abstract class PackageFragmentNode extends JavaElementNode {
    protected String name;
    protected Set<IPackageFragment> packageFragments;

    public PackageFragmentNode(String name, Set<? extends IPackageFragment> fragments) {
        this.name = name;
        this.packageFragments = Collections.unmodifiableSet(new LinkedHashSet<IPackageFragment>(fragments));
    }

    public Set<IPackageFragment> getPackageFragments() {
        return packageFragments;
    }

    @Override
    public IJavaElement toJavaElement() {
        //Just take the first since it's only for workbench rendering
        return packageFragments.isEmpty() ? null : (IJavaElement)packageFragments.iterator().next();
    }

    public String getElementName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageFragmentNode that = (PackageFragmentNode)o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
//        if (packageFragments != null ? !packageFragments.containsAll(that.packageFragments) : that.packageFragments != null)
//            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
//        result = 31 * result + (packageFragments != null ? packageFragments.hashCode() : 0);
        return result;
    }
}
