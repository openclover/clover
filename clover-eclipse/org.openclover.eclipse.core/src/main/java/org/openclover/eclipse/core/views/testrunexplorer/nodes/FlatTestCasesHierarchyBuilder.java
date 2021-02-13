package com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes;

import com.atlassian.clover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import com.atlassian.clover.eclipse.core.views.nodes.NodeRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

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
