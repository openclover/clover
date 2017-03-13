package com.atlassian.clover.eclipse.core.views.nodes;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

public abstract class BaseProjToPkgFragRelationship extends NodeRelationship {
    protected abstract boolean includePackage(IPackageFragment packageFragment) throws JavaModelException;
}
