package com.atlassian.clover.eclipse.testopt.editors.ruler;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.registry.ElementInfo;
import com.atlassian.clover.registry.entities.FullElementInfo;
import com.atlassian.clover.eclipse.core.ui.editors.java.CoverageAnnotation;
import com.atlassian.clover.eclipse.core.ui.editors.java.CoverageAnnotationModel;
import com.atlassian.clover.eclipse.core.ui.editors.java.ILineCoverageModel;
import com.atlassian.clover.eclipse.core.ui.editors.java.ILineCoverageModel.Entry;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.jface.text.source.AbstractRulerColumn;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;

import java.util.Collections;
import java.util.Set;

/**
 * Column used by text editor to display coverage annotations. It renders:
 *  - colour bar indicating lines: covered / covered by failed test / not covered (green/orange/red)
 *  - colour bar indicating that given line was touched by test case (dark green)
 *  - number of hit counts in given line
 */
public class CoverageAnnotationRulerColumn extends AbstractRulerColumn
        implements IContributedRulerColumn {
    /** The contribution descriptor. */
    private RulerColumnDescriptor fDescriptor;
    /** The target editor. */
    private ITextEditor fEditor;
    
    private CoverageAnnotationModel coverageAnnotationModel;

    /** Color for covered code */
    private final Color colorCovered;
    /** Color for covered code, but from failed test */
    private final Color colorFailCovered;
    /** Color for not covered code */
    private final Color colorNotCovered;
    /** Color for extra marker strip showing that given line was covered by passed test case */
    private final Color colorCoveredByPassedTest;
    /** Color for extra marker strip showing that given line was covered by failed test case */
    private final Color colorCoveredByFailedTest;
    
    private final IAnnotationModelListener annotationListener = new IAnnotationModelListener() {
        @Override
        public void modelChanged(IAnnotationModel model) {
            if (coverageAnnotationModel == null) {
                coverageAnnotationModel = CoverageAnnotationModel.getModel(fEditor);
            }
        }
    };

    public CoverageAnnotationRulerColumn() {
        AnnotationPreferenceLookup lookup = EditorsUI.getAnnotationPreferenceLookup();

        // read colors for: covered code, covered by failed test, not covered from user preferences
        RGB rgbCovered = lookup.getAnnotationPreference(CoverageAnnotation.Kind.COVERED.getId()).getColorPreferenceValue();
        RGB rgbFailCovered = lookup.getAnnotationPreference(CoverageAnnotation.Kind.FAILED.getId()).getColorPreferenceValue();
        RGB rgbNotCovered = lookup.getAnnotationPreference(CoverageAnnotation.Kind.NOT_COVERED.getId()).getColorPreferenceValue();
        RGB rgbTestPassedCovered = lookup.getAnnotationPreference(CoverageAnnotation.Kind.TEST_PASSED_COVERED.getId()).getColorPreferenceValue();
        RGB rgbTestFailedCovered = lookup.getAnnotationPreference(CoverageAnnotation.Kind.TEST_FAILED_COVERED.getId()).getColorPreferenceValue();

        colorCovered = rgbCovered == null ? getDefaultBackground() : EditorsUI.getSharedTextColors().getColor(rgbCovered);
        colorFailCovered = rgbCovered == null ? getDefaultBackground() : EditorsUI.getSharedTextColors().getColor(rgbFailCovered);
        colorNotCovered = rgbNotCovered == null ? getDefaultBackground() : EditorsUI.getSharedTextColors().getColor(rgbNotCovered);
        colorCoveredByPassedTest = rgbTestPassedCovered == null ? getDefaultBackground() : EditorsUI.getSharedTextColors().getColor(rgbTestPassedCovered);
        colorCoveredByFailedTest = rgbTestFailedCovered == null ? getDefaultBackground() : EditorsUI.getSharedTextColors().getColor(rgbTestFailedCovered);

        setTextInset(4);
        setHover(new CoverageAnnotationRulerHover(this));
    }

    @Override
    public void columnCreated() {
    }

    @Override
    public void columnRemoved() {
    }

    @Override
    public RulerColumnDescriptor getDescriptor() {
        return fDescriptor;
    }

    @Override
    public ITextEditor getEditor() {
        return fEditor;
    }

    @Override
    public void setDescriptor(RulerColumnDescriptor descriptor) {
        fDescriptor = descriptor;
    }

    @Override
    public void setEditor(ITextEditor editor) {
        fEditor = editor;
    }
    
    @Override
    public void setModel(IAnnotationModel model) {
        final IAnnotationModel currentModel = getModel();
        if (currentModel != null) {
            currentModel.removeAnnotationModelListener(annotationListener);
        }

        super.setModel(model);

        if (model != null) {
            model.addAnnotationModelListener(annotationListener);
        }
    }

    Entry getAnnotation(int lineNumber) {
        return coverageAnnotationModel == null || coverageAnnotationModel.getLineCoverageModel() == null ? null : coverageAnnotationModel.getLineCoverageModel().getForLine(lineNumber);
    }
    
    Set<TestCaseInfo> getPerTestInfo(Entry annotation) {
        CloverDatabase db = coverageAnnotationModel == null ? null : coverageAnnotationModel.cloverDatabaseForEditorInput();
        FullElementInfo info = getInfo(annotation);
        return db == null || info == null ? Collections.<TestCaseInfo>emptySet() : db.getTestHits(info);
    }

    @Override
    protected void paintLine(GC gc, int modelLine, int widgetLine,
            int linePixel, int lineHeight) {
        final Entry annotation = getAnnotation(modelLine);

        // skip coloring and writing hit counts if line is filtered-out; paint with default background instead
        if (computeFilter(modelLine)) {
            final Color emptyColor = super.computeBackground(modelLine);
            gc.setBackground(emptyColor);
            gc.fillRectangle(0, linePixel, getWidth(), lineHeight);
            return;
        }

        // get hit counts and colours for left/right half of the ruler for one row
        final String text = computeText(annotation);
        final Color leftBar = computeOptimisticBackground(modelLine);
        final Color rightBar = computePessimisticBackground(modelLine);
        // get color of test strip (or null)
        final Color barCode = computeRightTestStrip(annotation, modelLine);

        if (leftBar.equals(rightBar)) {
            gc.setBackground(leftBar);
            gc.fillRectangle(0, linePixel, getWidth(), lineHeight);
        } else {
            gc.setBackground(leftBar);
            gc.fillRectangle(0, linePixel, getWidth() / 2, lineHeight);
            gc.setBackground(rightBar);
            gc.fillRectangle(getWidth() / 2, linePixel, getWidth(), lineHeight);
        }

        if (barCode != null) {
            gc.setBackground(barCode);
            gc.fillRectangle(getWidth() - 4, linePixel, getWidth(), lineHeight);
        }

        if (text != null) {
            final int textWidth = gc.stringExtent(text).x;
            int minWidth = textWidth + 2 * getTextInset();
            if (getWidth() < minWidth) {
                setWidth(minWidth);
            }
            gc.setForeground(computeForeground(modelLine));
            gc.drawString(text, getWidth() - (textWidth + getTextInset()), linePixel, true);
        }
    }

    protected String computeText(Entry annotation) {
        final ElementInfo info = getInfo(annotation);
        return info == null ? null : String.valueOf(info.getHitCount());
    }
    
    FullElementInfo getInfo(Entry annotation) {
        return annotation == null ? null : annotation.getElementInfo();
    }
    
    @Override
    protected Color computeForeground(int line) {
        return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
    }

    /**
     * Checks whether given source line is filtered-out.
     * @param line number of source line
     * @return boolean - true if filtered-out, false otherwise
     */
    protected boolean computeFilter(int line) {
        if (coverageAnnotationModel != null) {
            ILineCoverageModel lineCoverageModel = coverageAnnotationModel.getLineCoverageModel();
            if (lineCoverageModel  != null) {
                return lineCoverageModel.isFilteredInLine(line);
            }
        }
        return false;
    }

    /**
     * Calculate color for left background, which shows an indicator whether at least part of line was covered.
     * It's an "optimistic marker". Coloring priority is as follows:
     *   1st colorCovered         if there are some passed tests or line was covered outside tests
     *   2nd colorFailedCovered   if line was covered but by failed tests only
     *   3rd colorNotCovered      if line was not covered at all
     * @param line number of source line
     * @return Color
     */
    protected Color computeOptimisticBackground(int line) {
        ILineCoverageModel lineCoverageModel =
                (coverageAnnotationModel != null) ? coverageAnnotationModel.getLineCoverageModel() : null;

        if (lineCoverageModel == null || lineCoverageModel.getForLine(line) == null) {
            return super.computeBackground(line); 
        } else {
            final boolean hasHitsInLine = lineCoverageModel.hasHitsInLine(line);
            final boolean hasPassedHitsInLine = lineCoverageModel.hasPassedHitsInLine(line);
            final boolean hasFailedHitsInLine = lineCoverageModel.hasFailedHitsInLine(line);
            // return color in a following priority: 1st covered, 2nd covered by failed test, 3rd not covered at all
            if ( hasPassedHitsInLine || (!hasFailedHitsInLine && hasHitsInLine) ) {
                return colorCovered;
            } else if (hasHitsInLine) {
                return colorFailCovered;
            } else {
                return colorNotCovered;
            }
        }
    }

    /**
     * Calculate color for right background, which shows an indicator whether at least part of line was NOT covered.
     * It's a "pessimistic marker". Coloring priority is as follows:
     *   1st colorNotCovered    if there are any coverage misses in line (i.e. partial line coverage)
     *   2nd colorFailedCovered if line was covered but by failed tests only
     *   3rd colorCovered       if there are some passed tests or line was covered outside tests
     * @param line number of source line
     * @return Color
     */
    protected Color computePessimisticBackground(int line) {
        ILineCoverageModel lineCoverageModel =
                (coverageAnnotationModel != null) ? coverageAnnotationModel.getLineCoverageModel() : null;

        if (lineCoverageModel == null || lineCoverageModel.getForLine(line) == null) {
            // use default background if there's no data
            return super.computeBackground(line); 
        } else {
            final boolean hasMissesInLine = lineCoverageModel.hasMissesInLine(line);
            final boolean hasFailedHitsInLine = lineCoverageModel.hasFailedHitsInLine(line);
            // return color in a following priority: 1st misses, 2nd covered by failed test, 3rd fully covered
            if (hasMissesInLine) {
                return colorNotCovered;
            } else if (hasFailedHitsInLine) {
                return colorFailCovered;
            } else {
                return colorCovered;
            }
        }
    }

    /**
     * Calculate color for narrow strip on the right edge of ruler. Non-null return value
     * indicates passed or failed test case; null - code not covered by test case.
     * @param annotation annotation related with given source line
     * @param line number of source line
     * @return Color null / colorCoveredByFailedTest / colorCoveredByPassedTest
     */
    protected Color computeRightTestStrip(Entry annotation, int line) {
        if (getPerTestInfo(annotation).isEmpty()) {
            return null; // null = no coloring
        } else {
            if (coverageAnnotationModel != null) {
                ILineCoverageModel lineCoverageModel = coverageAnnotationModel.getLineCoverageModel();
                if (lineCoverageModel != null && lineCoverageModel.hasFailedHitsInLine(line)) {
                    return colorCoveredByFailedTest;
                } else {
                    return colorCoveredByPassedTest;
                }
            }
            return null;
        }
    }

    @Override
    public void dispose() {
        colorCovered.dispose();
        colorFailCovered.dispose();
        colorCoveredByPassedTest.dispose();
        colorCoveredByFailedTest.dispose();
        colorNotCovered.dispose();
        
        super.dispose();
    }
}
