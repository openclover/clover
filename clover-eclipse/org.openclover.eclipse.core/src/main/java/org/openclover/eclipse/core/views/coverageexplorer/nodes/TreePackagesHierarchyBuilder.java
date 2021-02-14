package org.openclover.eclipse.core.views.coverageexplorer.nodes;

import org.openclover.eclipse.core.views.nodes.CUToTypeRelationship;
import org.openclover.eclipse.core.views.nodes.LogicalTreePkgFragNodeToTreeMultiPkgFragNodeAndCURelationship;
import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.ProjToTreePkgFragNodeRelationship;
import org.openclover.eclipse.core.views.nodes.TypeToInnerTypeAndMethodRelationship;
import org.openclover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

public class TreePackagesHierarchyBuilder extends NodeHierarchyBuilder {
    private static final NodeRelationship[] NODE_RELATIONSHIP = new NodeRelationship[]{
        new TypeToInnerTypeAndMethodRelationship(),
        new CUToTypeRelationship(),
        new LogicalTreePkgFragNodeToTreeMultiPkgFragNodeAndCURelationship(),
        new ProjToTreePkgFragNodeRelationship(),
        new WorkspaceToCloveredProjRelationship()
    };

    @Override
    public NodeRelationship[] getNodeRelationships() {
        return NODE_RELATIONSHIP;
    }
}
