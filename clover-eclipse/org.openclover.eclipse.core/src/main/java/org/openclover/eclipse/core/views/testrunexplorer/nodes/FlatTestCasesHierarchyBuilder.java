package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

public class FlatTestCasesHierarchyBuilder extends NodeHierarchyBuilder {
    private TestCaseNodeFactory tcnFactory;

    public FlatTestCasesHierarchyBuilder(TestCaseNodeFactory tcnFactory) {
        this.tcnFactory = tcnFactory;
    }

    @Override
    public NodeRelationship[] getNodeRelationships() {
        return new NodeRelationship[]{
            new ProjToTestCaseRelationship(tcnFactory),
            new WorkspaceToCloveredProjRelationship(),
        };
    }
}
