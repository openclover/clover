package org.openclover.eclipse.core.views.coverageexplorer.nodes;

import org.openclover.eclipse.core.views.nodes.CUToTypeRelationship;
import org.openclover.eclipse.core.views.nodes.LogicalTreePkgFragNodeToTreeMultiPkgFragNodeAndCURelationship;
import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.PkgFragRootToTreePkgFragNodeRelationship;
import org.openclover.eclipse.core.views.nodes.ProjToPkgFragRootRelationship;
import org.openclover.eclipse.core.views.nodes.TypeToInnerTypeAndMethodRelationship;
import org.openclover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

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
