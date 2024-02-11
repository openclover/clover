package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.IJavaElement;
import org.openclover.core.registry.CoverageDataProvider;
import org.openclover.eclipse.core.views.nodes.JavaElementNode;

public abstract class CoverageContributionNode extends JavaElementNode {
    private final IJavaElement element;
    private final float coverage;
    private final float unique;
    private final CoverageDataProvider testHits;
    private final CoverageDataProvider uniqueTestHits;

    public CoverageContributionNode(IJavaElement element, float coverage, float unique, CoverageDataProvider testHits, CoverageDataProvider uniqueTestHits) {
        this.element = element;
        this.coverage = coverage;
        this.unique = unique;
        this.testHits = testHits;
        this.uniqueTestHits = uniqueTestHits;
    }

    public IJavaElement getElement() {
        return element;
    }

    public float getCoverage() {
        return coverage;
    }

    public float getUnique() {
        return unique;
    }

    @Override
    public IJavaElement toJavaElement() {
        return element;
    }

    public CoverageDataProvider getTestHits() {
        return testHits;
    }

    public CoverageDataProvider getUniqueTestHits() {
        return uniqueTestHits;
    }
}
