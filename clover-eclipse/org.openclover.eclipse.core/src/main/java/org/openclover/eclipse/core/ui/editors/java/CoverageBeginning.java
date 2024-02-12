package org.openclover.eclipse.core.ui.editors.java;

import org.eclipse.jface.text.BadLocationException;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.eclipse.core.ui.editors.java.annotations.strategies.CoverageAnnotationBuilder;

class CoverageBeginning extends CoverageEdge {
    public CoverageBeginning(SourceInfo info) {
        super(info);
    }

    @Override
    public int getColumn() {
        return getInfo().getStartColumn();
    }

    @Override
    public int getLine() {
        return getInfo().getStartLine();
    }

    public boolean equals(Object o) {
        return (o instanceof CoverageBeginning) && super.equals(o);
    }

    @Override
    public void register(
        CoverageAnnotationBuilder builder,
        CoverageAnnotationFilter filter) throws BadLocationException {
        builder.onStartOfSourceRegion(getInfo(), filter.isHidden(getInfo()));
    }
}
