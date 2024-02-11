package org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space;

import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.registry.entities.FullElementInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.registry.CoverageDataRange;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotation;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageData;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.IDocument;

import java.util.Set;
import java.util.Collections;
import java.util.BitSet;
import java.util.Map;

abstract class AnnotationFragment {
    private final CloverDatabase database;
    private final IDocument document;
    private final SourceInfo region;
    private final Map<TestCaseInfo, BitSet> tcisAndHitsForFile;
    private final Set<TestCaseInfo> hits;
    private final boolean hidden;
    private final int startPos;
    private int length = -1;

    public AnnotationFragment(
        CloverDatabase database, IDocument document,
        SourceInfo region, Map<TestCaseInfo, BitSet> tcisAndHitsForFile,
        boolean hidden, int startPos) {

        this.database = database;
        this.document = document;
        this.region = region;
        this.tcisAndHitsForFile = tcisAndHitsForFile;
        this.hits = getTestHits(region);
        this.hidden = hidden;
        this.startPos = startPos;
    }

    public boolean isHidden() {
        return hidden;
    }

    public SourceInfo getRegion() {
        return region;
    }

    public void closeBefore(SourceInfo region) throws BadLocationException {
        length = DocumentUtils.lineColToOffset(document, region.getStartLine(), region.getStartColumn()) - startPos;
    }

    public void closeAfter(SourceInfo region) throws BadLocationException {
        length = DocumentUtils.lineColToOffset(document, region.getEndLine(), region.getEndColumn()) - startPos;
    }

    public CoverageAnnotation toAnnotation() {
        assertClosed();

        if (region instanceof FullElementInfo) {
            return
                CoverageAnnotation.Kind.kindFor(hidden, (ElementInfo)region, hits)
                    .newAnnotation((FullElementInfo)region, hits, startPos, length, hidden);
        } else {
            return null;
        }
    }

    public boolean compatibleWith(SourceInfo region, boolean hidden) {
        final Set<TestCaseInfo> thatRegionHits = getTestHits(region);
        return
            this.hidden == hidden
            && this.region.getClass() == region.getClass()
            && getHitCount(this.region) == getHitCount(region)
            && hits.containsAll(thatRegionHits)
            && thatRegionHits.containsAll(hits);
    }

    private int getHitCount(SourceInfo region) {
        return
            region instanceof ElementInfo
                ? ((ElementInfo)region).getHitCount()
                : 0;
    }

    private Set<TestCaseInfo> getTestHits(SourceInfo region) {
        if (region instanceof CoverageDataRange) {
            return CoverageData.tcisInHitRange(tcisAndHitsForFile, (CoverageDataRange)region);
        } else {
            return Collections.emptySet();
        }
    }

    public Position getPosition() {
        assertClosed();

        return new Position(startPos, length);
    }

    private void assertClosed() {
        if (!isClosed()) {
            throw new AssertionError("Annotation fragment is not closed");
        }
    }

    public boolean isClosed() {
        return length >= 0;
    }


    public String toString() {
        try {
            if (isClosed()) {
                int startLine = document.getLineOfOffset(startPos);
                int endLine = document.getLineOfOffset(startPos + length);

                return
                    "[" +  startLine + "," + (startPos - document.getLineOffset(startLine)) + "]"
                    + "->[" + endLine + "," + (startPos + length - document.getLineOffset(endLine)) + "]";
            } else {
            }
        } catch (BadLocationException e) {
        }
        return "[?,?]->[?,?]";
    }
}
