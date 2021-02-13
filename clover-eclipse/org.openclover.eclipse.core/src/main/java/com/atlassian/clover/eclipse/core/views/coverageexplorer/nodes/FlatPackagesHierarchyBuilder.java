package com.atlassian.clover.eclipse.core.views.coverageexplorer.nodes;

import com.atlassian.clover.eclipse.core.views.nodes.CUToTypeRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import com.atlassian.clover.eclipse.core.views.nodes.NodeRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.PkgFragToCURelationship;
import com.atlassian.clover.eclipse.core.views.nodes.ProjToLeafPkgFragRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.TypeToInnerTypeAndMethodRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

public class FlatPackagesHierarchyBuilder extends NodeHierarchyBuilder {
    private static final NodeRelationship[] NODE_RELATIONSHIP = new NodeRelationship[]{
        new TypeToInnerTypeAndMethodRelationship(),
        new CUToTypeRelationship(),
        new PkgFragToCURelationship(),
        new ProjToLeafPkgFragRelationship(),
        new WorkspaceToCloveredProjRelationship()
    };

    @Override
    public NodeRelationship[] getNodeRelationships() {
        return NODE_RELATIONSHIP;
    }
}
