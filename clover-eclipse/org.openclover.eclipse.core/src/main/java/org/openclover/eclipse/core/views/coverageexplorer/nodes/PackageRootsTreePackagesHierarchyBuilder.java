package com.atlassian.clover.eclipse.core.views.coverageexplorer.nodes;

import com.atlassian.clover.eclipse.core.views.nodes.CUToTypeRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import com.atlassian.clover.eclipse.core.views.nodes.NodeRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.PkgFragRootToTreePkgFragNodeRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.ProjToPkgFragRootRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.LogicalTreePkgFragNodeToTreeMultiPkgFragNodeAndCURelationship;
import com.atlassian.clover.eclipse.core.views.nodes.TypeToInnerTypeAndMethodRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

public class PackageRootsTreePackagesHierarchyBuilder extends NodeHierarchyBuilder {
    private static final NodeRelationship[] NODE_RELATIONSHIP = new NodeRelationship[]{
        new TypeToInnerTypeAndMethodRelationship(),
        new CUToTypeRelationship(),
        new LogicalTreePkgFragNodeToTreeMultiPkgFragNodeAndCURelationship(),
        new PkgFragRootToTreePkgFragNodeRelationship(),
        new ProjToPkgFragRootRelationship(),
        new WorkspaceToCloveredProjRelationship()
    };

    @Override
    public NodeRelationship[] getNodeRelationships() {
        return NODE_RELATIONSHIP;
    }
}
