package com.atlassian.clover.eclipse.core.ui.editors.java.annotations.strategies.space;

import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.CloverDatabase;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import java.util.BitSet;
import java.util.Map;

class AnnotationFragmentOnStart extends AnnotationFragment {
    public AnnotationFragmentOnStart(
        CloverDatabase database, IDocument document,
        SourceInfo region, Map<TestCaseInfo, BitSet> tcisAndHitsForFile,
        boolean hidden) throws BadLocationException {

        super(database, document, region, tcisAndHitsForFile, hidden, DocumentUtils.lineColToOffset(document, region.getStartLine(), region.getStartColumn()));
    }

}
