package com.atlassian.clover.eclipse.core.ui.editors.java.annotations.strategies.space;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.eclipse.core.ui.editors.java.CoverageAnnotation;

import java.util.SortedSet;
import java.util.BitSet;
import java.util.Map;

import org.eclipse.jface.text.IDocument;

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
