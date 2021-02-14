package org.openclover.eclipse.core.ui.editors.java;

import org.eclipse.jface.text.BadLocationException;
import org.openclover.eclipse.core.ui.editors.java.annotations.strategies.CoverageAnnotationBuilder;

class CoverageEnd extends CoverageEdge {
    public CoverageBeginning begining;

    public CoverageEnd(CoverageBeginning beginning) {
        super(beginning.getInfo());
        this.begining = beginning;
    }

    @Override
    public int getColumn() {
        return getInfo().getEndColumn();
    }

    @Override
    public int getLine() {
        return getInfo().getEndLine();
    }

    public boolean equals(Object o) {
        return super.equals(o) && begining.equals(((CoverageEnd) o).begining);
    }

    @Override
    public void register(
        CoverageAnnotationBuilder builder,
        CoverageAnnotationFilter filter) throws BadLocationException {
        builder.onEndOfSourceRegion(getInfo(), filter.isHidden(getInfo()));
    }
}
