package com.atlassian.clover.eclipse.core.views.nodes;

import java.util.Set;

public class LeafPackageFragmentNode extends PackageFragmentNode {
    public LeafPackageFragmentNode(String name, Set fragments) {
        super(name, fragments);
    }
}
