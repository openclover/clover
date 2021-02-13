package org.openclover.eclipse.testopt.editors.ruler;

import java.util.Collection;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHoverExtension;
import org.eclipse.jface.text.source.IAnnotationHoverExtension2;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineRange;

import com.atlassian.clover.eclipse.core.ui.editors.java.ILineCoverageModel.Entry;
import com.atlassian.clover.registry.entities.TestCaseInfo;

public class CoverageAnnotationRulerHover implements IAnnotationHover,
        IAnnotationHoverExtension, IAnnotationHoverExtension2 {

    private final IInformationControlCreator controlCreator = new CoverageAnnotationRulerHoverControlCreator();
    private final CoverageAnnotationRulerColumn coverageAnnotationRulerColumn;
    
    public CoverageAnnotationRulerHover(
            CoverageAnnotationRulerColumn coverageAnnotationRulerColumn) {
        this.coverageAnnotationRulerColumn = coverageAnnotationRulerColumn;
    }

    @Override
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
        throw new UnsupportedOperationException("This API should not be used");
    }

    @Override
    public boolean canHandleMouseCursor() {
        return false;
    }

    @Override
    public IInformationControlCreator getHoverControlCreator() {
        return controlCreator;
    }

    @Override
    public Object getHoverInfo(ISourceViewer sourceViewer,
            ILineRange lineRange, int visibleNumberOfLines) {
        
        final Entry annotation = coverageAnnotationRulerColumn.getAnnotation(lineRange.getStartLine());
        return annotation == null ? null : new CoverageAnnotationInput(annotation, coverageAnnotationRulerColumn.getPerTestInfo(annotation));
    }

    @Override
    public ILineRange getHoverLineRange(ISourceViewer viewer, int lineNumber) {
        return new LineRange(lineNumber, 1);
    }

    @Override
    public boolean canHandleMouseWheel() {
        return false;
    }

    public static class CoverageAnnotationInput {
        public final Collection<TestCaseInfo> testCases;
        public final Entry annotation;
        
        CoverageAnnotationInput(Entry annotation,
                Collection<TestCaseInfo> testCases) {
            this.annotation = annotation;
            this.testCases = testCases;
        }
        
    }
}
