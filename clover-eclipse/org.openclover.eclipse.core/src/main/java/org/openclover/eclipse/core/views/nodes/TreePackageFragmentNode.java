package com.atlassian.clover.eclipse.core.views.nodes;

import org.eclipse.jdt.core.IPackageFragment;

import java.util.Set;

public class TreePackageFragmentNode extends PackageFragmentNode {
    public TreePackageFragmentNode(String name, Set<? extends IPackageFragment> fragments) {
        super(name, fragments);
    }
}
