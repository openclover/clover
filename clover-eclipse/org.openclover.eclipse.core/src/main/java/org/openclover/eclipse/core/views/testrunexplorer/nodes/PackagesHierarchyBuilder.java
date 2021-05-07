package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.ProjToLeafPkgFragRelationship;
import org.openclover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

public class PackagesHierarchyBuilder extends NodeHierarchyBuilder {
    private TestCaseNodeFactory tcnFactory;

    public PackagesHierarchyBuilder(TestCaseNodeFactory tcnFactory) {
        this.tcnFactory = tcnFactory;
    }

    @Override
    public NodeRelationship[] getNodeRelationships() {
        return new NodeRelationship[]{
            new TypeToTestInnerTypeAndTestMethodRelationship(tcnFactory),
            new PkgFragToTypeRelationship(),
            new ProjToLeafPkgFragRelationship(),
            new WorkspaceToCloveredProjRelationship(),
        };
    }
}
