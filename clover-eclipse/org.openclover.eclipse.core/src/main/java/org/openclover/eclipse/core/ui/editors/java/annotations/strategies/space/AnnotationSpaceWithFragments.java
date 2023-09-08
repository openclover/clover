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

import static org.openclover.util.Lists.newLinkedList;

class AnnotationSpaceWithFragments extends AnnotationSpace {
    private AnnotationSpace parent;
    protected List<AnnotationFragment> fragments = newLinkedList();

    public AnnotationSpaceWithFragments(
        CloverDatabase database,
        IDocument document,
        Map<TestCaseInfo, BitSet> tcisAndHitsForFile,
        AnnotationSpace parent,
        SourceInfo region,
        boolean hidden)

        throws BadLocationException {
        super(database, document, tcisAndHitsForFile);
        this.parent = parent;
        foldIntoCurrentFragment(region, hidden);
    }

    @Override
    protected AnnotationSpace getParent() {
        return parent;
    }

    public boolean isOriginalFragmentStartedBy(SourceInfo region) {
        return getFirstFragment().getRegion() == region;
    }

    public void resumeWithNewAnnotationFragment(SourceInfo region) throws BadLocationException {
        fragments.add(
            new AnnotationFragmentOnResumption(
                database,
                document,
                getFirstFragment().getRegion(),
                tcisAndHitsForFile,
                getFirstFragment().isHidden(),
                DocumentUtils.lineColToOffset(document, region.getEndLine(), region.getEndColumn())));
    }


    @Override
    public AnnotationSpace newChildSpace(SourceInfo region, boolean hidden) throws BadLocationException {
        AnnotationSpace newSpace = super.newChildSpace(region, hidden);

        getLastFragment().closeBefore(region);

        return newSpace;
    }

    @Override
    public AnnotationSpace seal(SourceInfo region, boolean hidden) throws BadLocationException {
        super.seal(region, hidden);

        getLastFragment().closeAfter(region);

        if (parent instanceof AnnotationSpaceWithFragments) {
            ((AnnotationSpaceWithFragments)parent).resumeWithNewAnnotationFragment(region);
        }

        return parent;
    }


    @Override
    public SortedSet<CoverageAnnotation> toAnnotations(SortedSet<CoverageAnnotation> set) {
        super.toAnnotations(set);

        for (AnnotationFragment fragment : fragments) {
            set.add(fragment.toAnnotation());
        }

        return set;
    }

    public List<AnnotationFragment> getFragments() {
        return fragments;
    }

    private AnnotationFragment getLastFragment() {
        return fragments.size() == 0 ? null : fragments.get(fragments.size() - 1);
    }

    public boolean currentFragmentCompatibleWith(SourceInfo region, boolean hidden) {
        //First fragment's region is representative of all regions for the node
        return fragments.get(0).compatibleWith(region, hidden);
    }

    public void foldIntoCurrentFragment(SourceInfo region, boolean hidden) throws BadLocationException {
        ensureNotSealed();

        //If last fragment is closed then a new fragment is needed
        //This would happen immediately after a node is sealed and
        //represents the continuation of the enclosing node after
        //the rupture caused by the child node
        if (getLastFragment() == null || getLastFragment().isClosed()) {
            fragments.add(new AnnotationFragmentOnStart(database, document, region, tcisAndHitsForFile, hidden));
        }
    }

    public AnnotationFragment getFirstFragment() {
        return fragments.get(0);
    }

    public String toString() {
        try {
            return
                "AnnotationSpaceWithFragments([" + getFirstFragment().getRegion().getStartLine() + "," + getFirstFragment().getRegion().getStartColumn()
                    + "] -> [" + getLastFragment().getRegion().getEndLine() + "," + getLastFragment().getRegion().getEndColumn() + "],\n"
                    + (getFirstFragment().isHidden() ? " hidden" : "visible") + "\n"
                    + ">>>>\n"
                    + document.get(
                        DocumentUtils.lineColToOffset(document, getFirstFragment().getRegion().getStartLine(), getFirstFragment().getRegion().getStartColumn()),
                        (DocumentUtils.lineColToOffset(document, getLastFragment().getRegion().getEndLine(), getLastFragment().getRegion().getEndColumn())
                            - DocumentUtils.lineColToOffset(document, getFirstFragment().getRegion().getStartLine(), getFirstFragment().getRegion().getStartColumn())))
                    + "\n<<<<)\n";
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
