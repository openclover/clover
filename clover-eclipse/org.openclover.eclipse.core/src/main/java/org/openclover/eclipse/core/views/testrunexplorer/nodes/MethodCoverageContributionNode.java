package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.IMethod;
import org.openclover.core.api.registry.CoverageDataProvider;

public class MethodCoverageContributionNode extends CoverageContributionNode {
    public MethodCoverageContributionNode(IMethod method, float coverage, float unique, CoverageDataProvider testHits, CoverageDataProvider uniqueTestHits) {
        super(method, coverage, unique, testHits, uniqueTestHits);
    }
}
