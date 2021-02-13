package com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes;

import com.atlassian.clover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import com.atlassian.clover.eclipse.core.views.nodes.NodeRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.ProjToLeafPkgFragRelationship;
import com.atlassian.clover.eclipse.core.views.nodes.WorkspaceToCloveredProjRelationship;

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
