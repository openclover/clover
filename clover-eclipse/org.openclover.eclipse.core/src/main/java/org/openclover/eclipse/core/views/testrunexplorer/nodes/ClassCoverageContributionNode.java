package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.IType;
import org.openclover.core.registry.CoverageDataProvider;

public class ClassCoverageContributionNode extends CoverageContributionNode {
    public ClassCoverageContributionNode(IType type, float coverage, float unique, CoverageDataProvider testHits, CoverageDataProvider uniqueTestHits) {
        super(type, coverage, unique, testHits, uniqueTestHits);
    }
}
