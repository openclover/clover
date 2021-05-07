package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.PkgFragRootToPkgFragRelationship;
import org.openclover.eclipse.core.views.nodes.ProjToPkgFragRootRelationship;
import org.openclover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

public class PackageRootsHierarchyBuilder extends NodeHierarchyBuilder {
    private TestCaseNodeFactory tcnFactory;

    public PackageRootsHierarchyBuilder(TestCaseNodeFactory tcnFactory) {
        this.tcnFactory = tcnFactory;
    }

    @Override
    public NodeRelationship[] getNodeRelationships() {
        return new NodeRelationship[]{
            new TypeToTestInnerTypeAndTestMethodRelationship(tcnFactory),
            new PkgFragToTypeRelationship(),
            new PkgFragRootToPkgFragRelationship(),
            new ProjToPkgFragRootRelationship(),
            new WorkspaceToCloveredProjRelationship(),
        };
    }

}
