package org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.TestCaseInfo;

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
