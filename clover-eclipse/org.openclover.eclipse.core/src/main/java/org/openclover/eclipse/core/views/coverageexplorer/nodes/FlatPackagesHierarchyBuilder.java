package org.openclover.eclipse.core.views.coverageexplorer.nodes;

import org.openclover.eclipse.core.views.nodes.CUToTypeRelationship;
import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.PkgFragToCURelationship;
import org.openclover.eclipse.core.views.nodes.ProjToLeafPkgFragRelationship;
import org.openclover.eclipse.core.views.nodes.TypeToInnerTypeAndMethodRelationship;
import org.openclover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

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
