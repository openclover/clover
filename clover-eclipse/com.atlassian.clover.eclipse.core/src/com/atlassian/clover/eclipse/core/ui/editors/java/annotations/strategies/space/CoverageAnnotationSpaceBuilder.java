package com.atlassian.clover.eclipse.core.ui.editors.java.annotations.strategies.space;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.api.registry.StatementInfo;
import com.atlassian.clover.eclipse.core.ui.editors.java.annotations.strategies.CoverageAnnotationBuilder;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.ui.editors.java.CoverageAnnotation;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import java.util.SortedSet;
import java.util.BitSet;
import java.util.Map;

public class CoverageAnnotationSpaceBuilder implements CoverageAnnotationBuilder {
    private static final boolean DEBUG_ENABLED;
    static {
        DEBUG_ENABLED = CloverPlugin.isLoggingDebugFor("annotations");
    }
    
    private CloverDatabase database;
    private IDocument document;
    private AnnotationSpace root;
    private AnnotationSpace currentSpace;

    public CoverageAnnotationSpaceBuilder(CloverDatabase database, IDocument document, Map<TestCaseInfo, BitSet> tcisAndHitsForFile) throws BadLocationException {
        this.database = database;
        this.document = document;
        this.root = new RootAnnotationSpace(database, document, tcisAndHitsForFile);
        this.currentSpace = root;
    }

    @Override
    public void onStartOfSourceRegion(SourceInfo region, boolean hidden) throws BadLocationException {
        if (DEBUG_ENABLED) {
            System.out.println("ELEMENT START : " + toString(region));
        }

        if (currentSpace instanceof AnnotationSpaceWithFragments
            && ((AnnotationSpaceWithFragments) currentSpace).currentFragmentCompatibleWith(region, hidden)) {

            ((AnnotationSpaceWithFragments) currentSpace).foldIntoCurrentFragment(region, hidden);
        } else {
            if (DEBUG_ENABLED) {
                System.out.println(
                    "Beginning space: " + kind(region) + " [" + region.getStartLine() + "," + region.getStartColumn()
                    + "] -> [" + region.getEndLine() + "," + region.getEndColumn() + "]" + (hidden ? " (hidden" : ""));
                System.out.println(
                    ">>>>\n"
                    + document.get(
                        DocumentUtils.lineColToOffset(document, region.getStartLine(), region.getStartColumn()),
                        (DocumentUtils.lineColToOffset(document, region.getEndLine(), region.getEndColumn())
                            - DocumentUtils.lineColToOffset(document, region.getStartLine(), region.getStartColumn())))
                    + "\n<<<<\n");
            }

            currentSpace = currentSpace.newChildSpace(region, hidden);
        }
    }

    @Override
    public void onEndOfSourceRegion(SourceInfo region, boolean hidden) throws BadLocationException {
        if (DEBUG_ENABLED) {
            System.out.println("ELEMENT END : " + toString(region));
        }

        if (((AnnotationSpaceWithFragments) currentSpace).isOriginalFragmentStartedBy(region)) {
            if (DEBUG_ENABLED) {
                SourceInfo startRegion = ((AnnotationSpaceWithFragments)currentSpace).getFirstFragment().getRegion();

                System.out.println(
                    "Ending space: " + kind(startRegion) + " [" + startRegion.getStartLine() + "," + startRegion.getStartColumn()
                    + "] -> [" + region.getEndLine() + "," + region.getEndColumn() + "]" + (hidden ? " (hidden" : ""));
                String fragment = document.get(
                        DocumentUtils.lineColToOffset(document, startRegion.getStartLine(), startRegion.getStartColumn()),
                        (DocumentUtils.lineColToOffset(document, region.getEndLine(), region.getEndColumn())
                            - DocumentUtils.lineColToOffset(document, startRegion.getStartLine(), startRegion.getStartColumn())));
                if (fragment.startsWith("for (;;)")) {
                    System.out.println("PARENT: " + currentSpace.getParent().toString());
                }
                System.out.println(
                    ">>>>\n"
                    + fragment
                    + "\n<<<<\n");
            }

            currentSpace = currentSpace.seal(region, hidden);
        } else if (DEBUG_ENABLED && currentSpace instanceof AnnotationSpaceWithFragments) {
            boolean outOfPlace = true;
            for (AnnotationFragment current : ((AnnotationSpaceWithFragments) currentSpace).getFragments()) {
                if (current.getRegion() == region) {
                    outOfPlace = false;
                    break;
                }
            }

            if (outOfPlace) {
                throw new AssertionError(
                    "Region out of place: " + kind(region) + " [" + region.getStartLine() + "," + region.getStartColumn()
                    + "] -> [" + region.getEndLine() + "," + region.getEndColumn() + "]" + (hidden ? " (hidden" : "")
                    + "Current space: " + currentSpace.toString());
            }
        }
    }

    private String toString(SourceInfo region) throws BadLocationException {
        int startOffset = DocumentUtils.lineColToOffset(document, region.getStartLine(), region.getStartColumn());
        int endOffset = DocumentUtils.lineColToOffset(document, region.getEndLine(), region.getEndColumn());

        return
            kind(region) + " [" + region.getStartLine() + "," + region.getStartColumn() + "] -> ["
            + region.getEndLine() + "," + region.getEndColumn() + "]: "
            + charsWithElipses(document, 60, startOffset, endOffset);
    }

    private String kind(SourceInfo region) {
        if (region instanceof BaseClassInfo) {
            return "CLASS";
        } else if (region instanceof MethodInfo) {
            return "METHOD";
        } else if (region instanceof StatementInfo) {
            return "STATEMENT";
        } else if (region instanceof BranchInfo) {
            return "BRANCH";
        } else {
            return region.getClass().getName();
        }
    }

    private String charsWithElipses(IDocument document, int length, int startOffset, int endOffset) throws BadLocationException {
        String flattenedString = document.get(startOffset, endOffset - startOffset).replaceAll("\n", "\\\\n");

        if (flattenedString.length() <= length) {
            return "\"" + flattenedString + "\"";
        } else {
            int charsToShow = length - 3;
            int frontCharsToShow = charsToShow / 2;
            int backCharsToShow = charsToShow - frontCharsToShow;
            return
                "\"" + flattenedString.substring(0, frontCharsToShow)
                + "..."
                + flattenedString.substring(flattenedString.length() - backCharsToShow, flattenedString.length()) + "\"";
        }
    }

    @Override
    public SortedSet<CoverageAnnotation> toAnnotations(SortedSet<CoverageAnnotation> annotations) throws BadLocationException {
        return root.toAnnotations(annotations);
    }
}
