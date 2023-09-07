package org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space;

import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.CloverDatabase;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotation;

import java.util.List;
import java.util.SortedSet;
import java.util.BitSet;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import static clover.com.google.common.collect.Lists.newLinkedList;

public abstract class AnnotationSpace {
    protected CloverDatabase database;
    protected IDocument document;
    protected Map<TestCaseInfo, BitSet> tcisAndHitsForFile;
    protected List<AnnotationSpaceWithFragments> childSpaces;
    protected boolean sealed;

    protected AnnotationSpace(CloverDatabase database, IDocument document, Map<TestCaseInfo, BitSet> tcisAndHitsForFile) {
        this.database = database;
        this.document = document;
        this.tcisAndHitsForFile = tcisAndHitsForFile;
        this.childSpaces = newLinkedList();
        this.sealed = false;
    }

    public AnnotationSpace newChildSpace(SourceInfo region, boolean hidden) throws BadLocationException {
        ensureNotSealed();

        AnnotationSpaceWithFragments newSpace = new AnnotationSpaceWithFragments(database, document, tcisAndHitsForFile, this, region, hidden);
        childSpaces.add(newSpace);

        return newSpace;
    }

    public AnnotationSpace seal(SourceInfo region, boolean hidden) throws BadLocationException {
        ensureNotSealed();

        sealed = true;

        for (int i = 0; i < childSpaces.size(); i++) {
            AnnotationSpaceWithFragments annotationSpaceWithFragments = childSpaces.get(i);
            if (!annotationSpaceWithFragments.sealed) {
                throw new AssertionError("Child " + i + " not sealed: \n" + this);
            }
        }

        return getParent();
    }

    protected abstract AnnotationSpace getParent();

    public SortedSet<CoverageAnnotation> toAnnotations(SortedSet<CoverageAnnotation> set) {
        ensureSealed();

        for (AnnotationSpaceWithFragments childSpace : childSpaces) {
            childSpace.toAnnotations(set);
        }

        return set;
    }

    protected void ensureSealed() {
        if (!sealed) {
            throw new AssertionError("Node is not sealed: \n" + this);
        }
    }

    protected void ensureNotSealed() {
        if (sealed) {
            throw new AssertionError("Node is sealed: \n" + this);
        }
    }

}

