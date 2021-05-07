package org.openclover.eclipse.core.ui.editors.java;

import com.atlassian.clover.api.registry.SourceInfo;
import org.openclover.eclipse.core.ui.editors.java.annotations.strategies.CoverageAnnotationBuilder;
import org.eclipse.jface.text.BadLocationException;

abstract class CoverageEdge {
    private SourceInfo info;

    public CoverageEdge(SourceInfo info) {
        this.info = info;
    }

    public SourceInfo getInfo() {
        return info;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoverageEdge edge = (CoverageEdge) o;
        if (info.equals(edge.info)) return false;

        return true;
    }

    public int hashCode() {
        return info.hashCode();
    }

    public abstract void register(CoverageAnnotationBuilder builder, CoverageAnnotationFilter filter) throws BadLocationException;

    public abstract int getColumn();

    public abstract int getLine();
}
