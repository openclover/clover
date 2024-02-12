package org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space;

import org.eclipse.jface.text.IDocument;
import org.openclover.core.CloverDatabase;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotation;

import java.util.BitSet;
import java.util.Map;
import java.util.SortedSet;

class RootAnnotationSpace extends AnnotationSpace {
    public RootAnnotationSpace(
        CloverDatabase database,
        IDocument document,
        Map<TestCaseInfo, BitSet> tcisAndHitsForFile) {

        super(database, document, tcisAndHitsForFile);
    }

    @Override
    protected AnnotationSpace getParent() { return null; }

    @Override
    public SortedSet<CoverageAnnotation> toAnnotations(SortedSet<CoverageAnnotation> set) {
        sealed = true;
        return super.toAnnotations(set);
    }

    public String toString() {
        return "RootAnnotationSpace";
    }
}
