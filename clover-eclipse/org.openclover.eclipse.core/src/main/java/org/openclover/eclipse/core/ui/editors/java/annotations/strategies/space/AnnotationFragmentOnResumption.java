package org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space;

import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.CloverDatabase;
import org.eclipse.jface.text.IDocument;

import java.util.BitSet;
import java.util.Map;

class AnnotationFragmentOnResumption extends AnnotationFragment {
    public AnnotationFragmentOnResumption(
        CloverDatabase database, IDocument document,
        SourceInfo region, Map<TestCaseInfo, BitSet> tcisAndHitsForFile,
        boolean hidden, int startPos) {
        
        super(database, document, region, tcisAndHitsForFile, hidden, startPos);
    }
}
