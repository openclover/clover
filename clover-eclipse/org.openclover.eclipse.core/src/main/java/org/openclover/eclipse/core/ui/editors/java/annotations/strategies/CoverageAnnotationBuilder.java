package org.openclover.eclipse.core.ui.editors.java.annotations.strategies;

import com.atlassian.clover.api.registry.SourceInfo;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotation;
import org.eclipse.jface.text.BadLocationException;

import java.util.SortedSet;

public interface CoverageAnnotationBuilder {
    void onStartOfSourceRegion(SourceInfo region, boolean hidden) throws BadLocationException;

    void onEndOfSourceRegion(SourceInfo region, boolean hidden) throws BadLocationException;

    SortedSet<CoverageAnnotation> toAnnotations(SortedSet<CoverageAnnotation> annotations) throws BadLocationException;
}
