package com.atlassian.clover.idea.content;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ContextSet;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.entities.FullElementInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.util.MetricsFormatUtils;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.ConfigChangeEvent;
import com.atlassian.clover.idea.config.ConfigChangeListener;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.feature.CloverFeatures;
import com.atlassian.clover.idea.feature.FeatureEvent;
import com.atlassian.clover.idea.feature.FeatureListener;
import com.atlassian.clover.idea.feature.FeatureManager;
import com.atlassian.clover.idea.coverage.ModelUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.LineMarkerRenderer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static clover.com.google.common.collect.Lists.newArrayList;
import static com.atlassian.clover.idea.content.DocMarkupPlugin.CoverageBlock.Style.BAD;
import static com.atlassian.clover.idea.content.DocMarkupPlugin.CoverageBlock.Style.FAILED_ONLY;
import static com.atlassian.clover.idea.content.DocMarkupPlugin.CoverageBlock.Style.FILTERED;
import static com.atlassian.clover.idea.content.DocMarkupPlugin.CoverageBlock.Style.GOOD;
import static com.atlassian.clover.idea.content.DocMarkupPlugin.CoverageBlock.Style.OLD;

public class DocMarkupPlugin extends ContentPlugin implements FeatureListener, ConfigChangeListener {

    private final List<RangeHighlighter> highlights = newArrayList();

    private boolean enabled;

    public DocMarkupPlugin(Project proj, VirtualFile vf) {
        super(proj, vf);
    }

    @Override
    public void install(Editor e) {
        super.install(e);

        FeatureManager fManager = ProjectPlugin.getPlugin(project).getFeatureManager();
        fManager.addFeatureListener(CloverFeatures.CLOVER_REPORTING, this);

        enabled = fManager.isFeatureEnabled(CloverFeatures.CLOVER_REPORTING);

        ProjectPlugin.getPlugin(project).getConfig().addConfigChangeListener(this);
        updateMarkups();
    }

    @Override
    public void uninstall() {
        ProjectPlugin.getPlugin(project).getConfig().removeConfigChangeListener(this);

        FeatureManager fManager = ProjectPlugin.getPlugin(project).getFeatureManager();
        fManager.removeFeatureListener(CloverFeatures.CLOVER_REPORTING, this);

        clearMarkups();

        super.uninstall();
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if ( evt.hasPropertyChange(IdeaCloverConfig.SHOW_ERROR_MARKS) ||
                evt.hasPropertyChange(IdeaCloverConfig.SHOW_GUTTER) ||
                evt.hasPropertyChange(IdeaCloverConfig.SHOW_INLINE) ||
                evt.hasPropertyChange(IdeaCloverConfig.HIGHLIGHT_COVERED) ||
                evt.hasPropertyChange(IdeaCloverConfig.COVERED_HIGHLIGHT) ||
                evt.hasPropertyChange(IdeaCloverConfig.COVERED_STRIPE) ||
                evt.hasPropertyChange(IdeaCloverConfig.NOT_COVERED_HIGHLIGHT) ||
                evt.hasPropertyChange(IdeaCloverConfig.NOT_COVERED_STRIPE) ||
                evt.hasPropertyChange(IdeaCloverConfig.FAILED_COVERED_HIGHLIGHT) ||
                evt.hasPropertyChange(IdeaCloverConfig.FAILED_COVERED_STRIPE) ||
                evt.hasPropertyChange(IdeaCloverConfig.FILTERED_HIGHLIGHT) ||
                evt.hasPropertyChange(IdeaCloverConfig.FILTERED_STRIPE) ||
                evt.hasPropertyChange(IdeaCloverConfig.OUTOFDATE_HIGHLIGHT) ||
                evt.hasPropertyChange(IdeaCloverConfig.OUTOFDATE_STRIPE) || 
                evt.hasPropertyChange(IdeaCloverConfig.INCLUDE_PASSED_TEST_COVERAGE_ONLY) ) {
            updateMarkups();
        }
    }

    /**
     * @see com.atlassian.clover.idea.feature.FeatureListener#featureStateChanged(com.atlassian.clover.idea.feature.FeatureEvent)
     */
    @Override
    public void featureStateChanged(FeatureEvent evt) {
        if (enabled != evt.isEnabled()) {
            enabled = evt.isEnabled();
            updateMarkups();
        }
    }

    private void updateMarkups() {

        clearMarkups();

        if (!enabled) {
            return;
        }

        if (coverageInfo == null) {
            return;
        }

        if (installedEditor == null) {
            return;
        }

        if (isCoverageUpToDate()) {
            final FullFileInfo fileInfo;
            final CloverDatabase cloverDatabase = currentCoverageModel != null ? currentCoverageModel.getCloverDatabase() : null;

            if (cloverDatabase != null && ModelUtil.isPassedTestsCoverageOnly(cloverDatabase)) {
                fileInfo = coverageInfo.copy((FullPackageInfo) coverageInfo.getContainingPackage(), HasMetricsFilter.ACCEPT_ALL);
            } else {
                fileInfo = coverageInfo;
            }
            new HighlightMarkupBuilder(ProjectPlugin.getPlugin(project).getConfig()).process(fileInfo);
        } else {
            if (hasPotentialCloverableElements()) {
                final int end = installedEditor.getDocument().getTextLength();
                RangeHighlighter rangeHighlighter =
                        installedEditor.getMarkupModel().addRangeHighlighter(0, end, 0, null, HighlighterTargetArea.LINES_IN_RANGE);
                final Color c = ProjectPlugin.getPlugin(project).getConfig().getOutOfDateStripe();
                rangeHighlighter.setLineMarkerRenderer(new CloverRangeMarkerRenderer(c));
                highlights.add(rangeHighlighter);
            }
        }

    }


    private class HighlightMarkupBuilder {
        public final boolean showGutter;
        public final boolean showError;
        public final boolean showHighlight;
        public final boolean highlightCovered;
        public final boolean includeFailedCoverage;

        public final Color coveredHighlight;
        public final Color coveredStripe;
        public final Color notCoveredHighlight;
        public final Color notCoveredStripe;
        public final Color outOfDateHighlight;
        public final Color outOfDateStripe;

        //todo this should actually be configurable
        public final Color excludedHighlight;
        public final Color excludedStripe;
        public final Color failedOnlyHighlight;
        public final Color failedOnlyStripe;

        private final MarkupModel markup = installedEditor.getMarkupModel();
        private final Document doc = installedEditor.getDocument();

        private final boolean uptodate = isCoverageUpToDate();

        private final CoverageBlock.Style[] lineStatus = new CoverageBlock.Style[doc.getLineCount()];

        private ContextSet filter;

        private final CoverageDataProvider fullDataProvider;
        private final CoverageDataProvider passedTestDataProvider;

        private HighlightMarkupBuilder(final IdeaCloverConfig config) {
            final CloverDatabase cloverDatabase = currentCoverageModel != null ? currentCoverageModel.getCloverDatabase() : null;
            fullDataProvider = cloverDatabase != null ? cloverDatabase.getCoverageData() : null;
            passedTestDataProvider = currentCoverageModel != null ? currentCoverageModel.getCachedPassOnlyCoverage() : null;

            // copy current clover configuration to private fields for faster access
            showGutter = config.isShowGutter();
            showError = config.isShowErrorMarks();
            showHighlight = config.isShowInline();
            highlightCovered = config.isHighlightCovered();

            coveredHighlight = config.getCoveredHighlight();
            notCoveredHighlight = config.getNotCoveredHighlight();
            notCoveredStripe = config.getNotCoveredStripe();
            outOfDateStripe = config.getOutOfDateStripe();
            outOfDateHighlight = config.getOutOfDateHighlight();
            coveredStripe = config.getCoveredStripe();

            excludedHighlight = config.getFilteredHighlight();
            excludedStripe = config.getFilteredStripe();
            failedOnlyHighlight = config.getFailedCoveredHighlight();
            failedOnlyStripe = config.getFailedCoveredStripe();
            includeFailedCoverage = !config.isIncludePassedTestCoverageOnly();
        }

        /**
         * Process document markups (background coloring, gutter lines) for given file.
         * @param coverageInfo       coverage information for one source file
         */
        void process(final FullFileInfo coverageInfo) {
            filter = coverageInfo.getContextFilter();
            if (showHighlight || showError || showGutter) {
                for (SourceInfo o : coverageInfo.getSourceRegions()) {
                    if (o instanceof FullElementInfo) {
                        if (showHighlight || showError) {
                            highlightStatement((FullElementInfo) o);
                        }
                        if (showGutter) {
                            markLines((FullElementInfo) o);
                        }
                    }
                }

                if (showGutter) {
                    createGutterMarks();
                }
            }
        }

        /**
         * Highlight single statement.
         * @param info  element to be processed
         */
        private void markLines(FullElementInfo info) {

            if (uptodate) {
                final boolean isFiltered = info.isFiltered(filter);
                final boolean passedOnlyHit = passedTestDataProvider.getHitCount(info.getDataIndex()) > 0;
                final boolean anyHit = fullDataProvider.getHitCount(info.getDataIndex()) > 0;

                for (int i = info.getStartLine() - 1; i < info.getEndLine(); ++i) {
                    if (isFiltered) {
                        lineStatus[i] = FILTERED;
                    } else if (passedOnlyHit) {
                        if (lineStatus[i] == null) {
                            // Filtered and Bad have higher priority
                            lineStatus[i] = GOOD;
                        }
                    } else if (anyHit && includeFailedCoverage) {
                        if (lineStatus[i] == null || lineStatus[i] == GOOD) {
                            // Filtered and Bad have higher priority
                            lineStatus[i] = FAILED_ONLY;
                        }

                    } else {
                        lineStatus[i] = BAD;
                    }
                }
            } else {
                for (int i = info.getStartLine() - 1; i < info.getEndLine(); ++i) {
                    lineStatus[i] = OLD;
                }
            }
        }

        /**
         * Highlight single statement.
         * @param info  element to be processed
         */
        private void highlightStatement(final FullElementInfo info) {

            int statementStart = doc.getLineStartOffset(info.getStartLine() - 1) + info.getStartColumn() - 1;
            int statementEnd = doc.getLineStartOffset(info.getEndLine() - 1) + info.getEndColumn() - 1;

            // workaround for CLOV-200
            if (statementStart > statementEnd) {
                int v = statementStart;
                statementStart = statementEnd;
                statementEnd = v;
            }
            final TextAttributes statementTextAttributes = new TextAttributes();
            RangeHighlighter statementHighlight = markup.addRangeHighlighter(statementStart, statementEnd,
                                                                             HighlighterLayer.WARNING, statementTextAttributes, HighlighterTargetArea.EXACT_RANGE);
            final Color bgColor;
            final Color stripeColor;

            // can't use info.getHitCount as it may use the wrong data provider
            final boolean passedOnlyHit = passedTestDataProvider.getHitCount(info.getDataIndex()) > 0;
            final boolean anyHit = fullDataProvider.getHitCount(info.getDataIndex()) > 0;
            boolean isFiltered = false;
            if (uptodate) {
                isFiltered = info.isFiltered(filter);

                if (isFiltered) {
                    bgColor = excludedHighlight;
                    stripeColor = excludedStripe;
                } else if (passedOnlyHit) {
                    bgColor = coveredHighlight;
                    stripeColor = coveredStripe;
                } else if (anyHit && includeFailedCoverage) {
                    bgColor = failedOnlyHighlight;
                    stripeColor = failedOnlyStripe;
                } else {
                    bgColor = notCoveredHighlight;
                    stripeColor = notCoveredStripe;
                }
            } else {
                bgColor = outOfDateHighlight;
                stripeColor = outOfDateStripe;
            }
            if (showHighlight) {
                if (!anyHit || highlightCovered || isFiltered) {
                    statementTextAttributes.setBackgroundColor(bgColor);
                }
                if (info instanceof BranchInfo) {
                    BranchInfo branchInfo = (BranchInfo) info;
                    statementTextAttributes.setEffectType(EffectType.LINE_UNDERSCORE);
                    Color underlineColor = branchInfo.getFalseHitCount() != 0 && branchInfo.getTrueHitCount() != 0 ?
                            coveredStripe : notCoveredStripe;
                    statementTextAttributes.setEffectColor(underlineColor);
                }
            }
            if (showError) {
                statementHighlight.setErrorStripeMarkColor(stripeColor);
                statementHighlight.setErrorStripeTooltip(uptodate ? MetricsFormatUtils.textForCoverage(info) :
                        "Coverage data out of date");
            }

            highlights.add(statementHighlight); // in containing class; ugly
        }

        private List<CoverageBlock> aggregateLines() {
            List<CoverageBlock> styles = newArrayList();

            int line = 0;

            while (line < lineStatus.length) {
                CoverageBlock current = new CoverageBlock();
                current.style = lineStatus[line];
                current.startLine = line;
                do {
                    ++line;
                } while (line < lineStatus.length && current.style == lineStatus[line]);
                current.endLine = line - 1;
                styles.add(current);
            }

            return styles;
        }

        private void createGutterMarks() {
            if (!showGutter) {
                return;
            }

            Map<CoverageBlock.Style, Color> colorMap = new EnumMap<CoverageBlock.Style, Color>(CoverageBlock.Style.class);
            colorMap.put(BAD, notCoveredStripe);
            colorMap.put(FILTERED, excludedStripe);
            colorMap.put(GOOD, coveredStripe);
            colorMap.put(FAILED_ONLY, failedOnlyStripe);
            colorMap.put(OLD, outOfDateStripe);

            List<CoverageBlock> styles = aggregateLines();

            for (CoverageBlock block : styles) {
                if (block.getStyle() == null) {
                    continue;
                }
                final int startOffset = installedEditor.getDocument().getLineStartOffset(block.getStartLine());
                final int endOffset = installedEditor.getDocument().getLineEndOffset(block.getEndLine());
                RangeHighlighter rangeHighlight =
                        markup.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.GUARDED_BLOCKS, null, HighlighterTargetArea.LINES_IN_RANGE);
                rangeHighlight.setLineMarkerRenderer(new CloverRangeMarkerRenderer(colorMap.get(block.getStyle())));
                highlights.add(rangeHighlight);
            }
        }
    }

    static class CoverageBlock {
        enum Style {
            GOOD, FAILED_ONLY, BAD, OLD, FILTERED
        }

        public Style style;
        public int startLine;
        public int endLine;

        public int getStartLine() {
            return startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public int getLength() {
            return getEndLine() - getStartLine() + 1;
        }

        public Style getStyle() {
            return style;
        }
    }

    private void clearMarkups() {
        if (installedEditor != null) {    // hack - bandaid fix to some fucked up race condition that I can't be bothered debugging
            MarkupModel markup = installedEditor.getMarkupModel();
            for (RangeHighlighter highlighter : highlights) {
                if (highlighter != null) {
                    markup.removeHighlighter(highlighter);
                }
            }
        }
        highlights.clear();

    }

    @Override
    public void refresh() {
        updateMarkups();
    }
}

class CloverRangeMarkerRenderer extends BoundedMarkRenderer implements LineMarkerRenderer {
    public CloverRangeMarkerRenderer(Color c) {
        super(c);
        setDrawTop(true);
        setDrawBase(true);
    }

    @Override
    public void paint(Editor editor, Graphics g, Rectangle r) {
        r.height += editor.getLineHeight(); // workaround for IDEA weirdness - rectangle covers all lines but the last
        r.width += 1; // mimic old behavior
        paint(g, r);
    }
}

interface MarkRenderer {
    void paint(Graphics graphics, Rectangle r);
}

class BoundedMarkRenderer implements MarkRenderer {

    private Color markColour;
    private boolean drawTop = true;
    private boolean drawBase;

    public BoundedMarkRenderer(Color c) {
        markColour = c;
    }

    public void setDrawTop(boolean b) {
        drawTop = b;
    }

    public void setDrawBase(boolean b) {
        drawBase = b;
    }

    @Override
    public void paint(Graphics graphics, Rectangle r) {

        Color save = graphics.getColor();
        try {
            graphics.setColor(markColour);

            // draw left edge.
            graphics.fillRect((int) r.getX(), (int) r.getY(), 2, (int) r.getHeight());

            if (drawTop) {
                graphics.fillRect((int) r.getX() + 2, (int) r.getY(), (int) r.getWidth() - 2, 2);
            }

            if (drawBase) {
                graphics.fillRect((int) r.getX() + 2, (int) r.getY() + (int) r.getHeight() - 2, (int) r.getWidth() - 2, 2);
            }

        } finally {
            graphics.setColor(save);
        }
    }
}
