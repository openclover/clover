package org.openclover.eclipse.core.views.nodes;

import org.eclipse.jdt.core.IPackageFragment;

public abstract class BaseProjToPkgFragRelationship extends NodeRelationship {
    protected abstract boolean includePackage(IPackageFragment packageFragment);
}
