package org.openclover.core.reporters.html.source;

import clover.org.apache.commons.lang3.StringUtils;
import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.registry.entities.BasicElementInfo;
import org.openclover.core.registry.entities.FullElementInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.LineInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;
import org.openclover.core.reporters.html.JSONObjectFactory;
import org.openclover.core.spi.reporters.html.source.LineRenderInfo;
import org.openclover.core.spi.reporters.html.source.SourceRenderer;
import org.openclover.core.util.ChecksummingReader;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.IOStreamUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.openclover.core.spi.reporters.html.source.SourceReportCss.BAD_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.COVERAGE_COUNT_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.FILTERED_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.GOOD_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.HIT_BY_FAILED_TEST_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.HIT_BY_TEST_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.LINE_COUNT_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.LINE_WARNING_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.MISSED_BY_TEST_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.NO_HILIGHT_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.SRC_LINE_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.SRC_LINE_FILTERED_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.SRC_LINE_HILIGHT_CLASS;
import static org.openclover.core.util.Lists.newArrayList;

public class SourceRenderHelper {
    private CloverDatabase database;
    private CoverageDataProvider coverageProvider;
    private Current report;
    private HtmlRenderingSupportImpl renderingHelper;
    private boolean outOfDate;
    private final String spaceChar;
    private final String tabStr;

    public SourceRenderHelper(CloverDatabase database, Current report, HtmlRenderingSupportImpl renderingHelper) {
        this.database = database;
        // store reference to "pure" data; will be used to reset coverage provider at file level
        // gatherSrcRenderInfo() method; we take data provider from database.getFullModel() instead of
        // database.getCoverageData() because full model can have filtering-out of failed test cases applied
        this.coverageProvider = database.getFullModel().getDataProvider();
        this.report = report;
        this.renderingHelper = renderingHelper;
        this.spaceChar = StringUtils.defaultIfEmpty(report.getFormat().getSpaceChar(), " ");
        this.tabStr = StringUtils.repeat(spaceChar, report.getFormat().getTabWidth());
    }

    public void insertLineInfosForFile(FullFileInfo fileInfo, VelocityContext context, ContextSet contextSet, String emptyChar, List[] testLineInfo) {
        try {
            LineRenderInfo[] renderInfo = gatherSrcRenderInfo(context, fileInfo, contextSet, emptyChar, testLineInfo);
            context.put("renderInfo", renderInfo);
            context.put("jsonSrcFileLines", JSONObjectFactory.getJSONSrcFileLines(renderInfo, fileInfo.getName()));
            if (outOfDate) {
                addWarning(context, RenderMessages.OUT_OF_DATE);
                Logger.getInstance().warn("Source file " + fileInfo.getPhysicalFile()
                    + " has changed since coverage information was"
                    + " generated");
            }
        } catch (FileNotFoundException e) {
            Logger.getInstance().error(e);
            putErrorMessage(context, "Clover could not read the source file \"" + fileInfo.getPhysicalFile().getAbsolutePath() + "\"");
        } catch (Exception e) {
            Logger.getInstance().error(e);
            putErrorMessage(context, RenderMessages.FAILED_RENDERING);
        }
    }

    private void putErrorMessage(VelocityContext context, String message) {
        context.put("errormsg", message);
    }

    @SuppressWarnings("unchecked")
    private void addWarning(VelocityContext context, String message) {
        List<String> warningMessages = (List<String>) context.get("warningMessages");
        if (warningMessages == null) {
            warningMessages = newArrayList();
        }
        warningMessages.add(message);
        context.put("warningMessages", warningMessages);
    }

    /**
     * WARN: the FullFileInfo object passed here will have another data provider set on it.
     * It is up to callers to ensure that a copy is passed - this is for performance reasons only.
     *
     * @param vc        velocity context
     * @param finfo     a writable copy of the file info to gather the render info for
     * @param emptyCoverageChar the String to use when there is no coverage
     * @return each line with rendering info added
     * @throws java.io.IOException if an error occurs rendering this file page
     * @throws clover.antlr.TokenStreamException
     *                             if an error occurs reading the source file
     */
    public LineRenderInfo[] gatherSrcRenderInfo(VelocityContext vc, FullFileInfo finfo, ContextSet contextSet,
                                                String emptyCoverageChar, List<TestCaseInfo>[] testLineInfo)
        throws Exception {
        // remove the failed test coverage filter at the file level...
        finfo.setDataProvider(this.coverageProvider);
        final int lineCount = finfo.getLineCount();
        List<LineRenderInfo> renderedLines = new ArrayList<>(lineCount);

        ChecksummingReader csr;
        try {
            SourceRenderer renderer = SourceRendererManager.getRendererForFileExtension(extensionOf(finfo.getName()));
            if (renderer == null) {
                Logger.getInstance().debug("No renderer registered for files with extension \"" + extensionOf(finfo.getName()) + "\". Using plaintext renderer for file " + finfo.getName());
                renderer = SourceRendererManager.getPlaintextRenderer();
            } else {
                Logger.getInstance().debug("Found source renderer " + renderer.getClass().getName() + " for file " + finfo.getName());
            }

            csr = render(finfo, renderedLines, emptyCoverageChar, renderer);
        } catch (Throwable t) {
            Logger.getInstance().error("Failed to render syntax highlights for " + finfo.getPhysicalFile().getAbsolutePath() + ": " + t.getMessage(), t);

            putErrorMessage(vc, RenderMessages.FALLBACK_RENDERING);
            //Start again but with plain text rendering this time
            renderedLines.clear();

            csr = render(finfo, renderedLines, emptyCoverageChar, SourceRendererManager.getPlaintextRenderer());
        }

        //We need at least as many LineInfos as we have lines to render. They are initialised with defaults
        //that render them as uncovered lines. This is all necessary because in Groovy land enums seem to loose their
        //line/col numbers but the methods they contain do not. We
        final LineInfo[] lines = finfo.getLineInfo(renderedLines.size() + 1, report.isShowLambdaFunctions(),
                report.isShowInnerFunctions());
        for (int i = 0; i < renderedLines.size(); i++) {
            boolean hasSomeCoverage = false;
            boolean hilightInfo = false;
            boolean hilightBad = false;
            ContextSet filteredCtx = null;

            String msg = "";
            int headlineHits = -1;
            String ccstr = emptyCoverageChar;
            LineInfo linfo = lines[i + 1];

            finishLine: {
                if (linfo != null) {
                    final List<FullElementInfo<? extends BasicElementInfo>> lineElements = linfo.getColumnOrderedElementInfos();

                    //Initial scan to just detect if:
                    //* the line is filtered
                    //* the line has *any* coverage
                    for (FullElementInfo lineElement : lineElements) {
                        if (filteredCtx != null || lineElement.isFiltered(contextSet)) {
                            filteredCtx = lineElement.getContext();
                            //Game over - filtered out
                            break finishLine;
                        }

                        if (lineElement.getHitCount() > 0) {
                            hasSomeCoverage = true;
                            break;
                        }
                    }

                    //Subsequent scan to find in order of precedence:
                    // * Element without full coverage
                    // * Element with full coverage
                    //
                    // We stop at the first uncovered element or last covered element - whichever comes first
                    for (FullElementInfo lineElement : lineElements) {
                        if (noHits(lineElement)) {
                            //First zero-hit element
                            String[] messages = calcCoverageMsg(lineElement, emptyCoverageChar);
                            msg = messages[0];
                            ccstr = messages[1];
                            headlineHits = hitCounts(lineElement);
                            hilightBad = true;
                            //Game over - uncovered elements trump preceding and following covered elements
                            break finishLine;
                        } else /*if (headlineHits == -1)*/ {
                            //First element seen for line...
                            String[] messages = calcCoverageMsg(lineElement, emptyCoverageChar);
                            msg = messages[0];
                            ccstr = messages[1];
                            headlineHits = hitCounts(lineElement);
                            //Now continue until a zero-hit element is found
                        }
                    }
                }
            }
            
            if (headlineHits >= 0) {
                hilightInfo = true;
                if (ccstr.equals(emptyCoverageChar) && filteredCtx == null) {
                    ccstr = "" + headlineHits;
                }
            }

            if (renderedLines.size() <= i || renderedLines.get(i) == null) {
                renderedLines.add(new LineRenderInfo(emptyCoverageChar));
            }
            
            LineRenderInfo thisLine = renderedLines.get(i);
            if (linfo != null && linfo.hasClassStarts()) {
                // HACK just get the first class for the line, if any
                thisLine.setClassStart(linfo.getClassStarts()[0]);
            }
            if (linfo != null && linfo.hasMethodStarts()) {
                // HACK just get the first method for the line, if any
                thisLine.setMethodStart(linfo.getMethodStarts()[0]);
            }
            if (linfo != null && linfo.hasFailStackEntries()) {
                // HACK leaking array ref here
                thisLine.setFailedStackEntries(linfo.getFailStackEntries());
            }
            boolean classStart = thisLine.getClassStart() != null;

            // determine if a line was only hit by one or more failing tests.
            String hitClass = MISSED_BY_TEST_CLASS;

            final List<TestCaseInfo> testsForLine = (testLineInfo == null || (i + 1 >= testLineInfo.length)) ?
                    null : testLineInfo[i + 1];
            if (testsForLine != null) {
                for (TestCaseInfo tci : testsForLine) {
                    if (!tci.isSuccess()) {
                        hitClass = HIT_BY_FAILED_TEST_CLASS;
                    } else {
                        hitClass = HIT_BY_TEST_CLASS;
                        break; // found at least one test that passed.
                    }
                }
            }

            if (filteredCtx != null) {
                thisLine.setLineNumberCSS(LINE_COUNT_CLASS + " " + FILTERED_CLASS);
                thisLine.setCoverageCountCSS(COVERAGE_COUNT_CLASS + " " + FILTERED_CLASS);
                thisLine.setTestHitCSS(MISSED_BY_TEST_CLASS);
                thisLine.setSourceCSS(SRC_LINE_FILTERED_CLASS);
                thisLine.setFiltered(true);
                filteredCtx = filteredCtx.and(contextSet);

                //TODO: NB - getContextAsString actually reads and writes to a HashMap cache.
                String contextString = database.getContextStore().getContextsAsString(filteredCtx);
                msg = "Filtered by: " + renderingHelper.htmlEscapeStr(contextString);

            } else if (hilightBad) {
                thisLine.setLineNumberCSS(LINE_COUNT_CLASS + " " + (hasSomeCoverage ? GOOD_CLASS : BAD_CLASS));
                thisLine.setCoverageCountCSS(COVERAGE_COUNT_CLASS + " " + BAD_CLASS);
                thisLine.setSourceCSS(SRC_LINE_HILIGHT_CLASS);
                thisLine.setTestHitCSS(hitClass);
            } else if (hilightInfo) {
                thisLine.setLineNumberCSS(LINE_COUNT_CLASS + " " + GOOD_CLASS);
                thisLine.setCoverageCountCSS(COVERAGE_COUNT_CLASS + " " + GOOD_CLASS);
                thisLine.setSourceCSS(SRC_LINE_CLASS);
                thisLine.setTestHitCSS(hitClass);
            } else {
                thisLine.setLineNumberCSS(LINE_COUNT_CLASS + " " + NO_HILIGHT_CLASS);
                thisLine.setCoverageCountCSS(COVERAGE_COUNT_CLASS + " " + NO_HILIGHT_CLASS);
                thisLine.setSourceCSS(SRC_LINE_CLASS);
            }

            if (outOfDate) {
                thisLine.setLineNumberCSS(LINE_WARNING_CLASS);
            }

            thisLine.setHilight(hilightInfo && (filteredCtx == null));
            thisLine.setCoverageStr(ccstr);
            thisLine.setMsg(msg);
            thisLine.setTestHits(testsForLine != null ? testsForLine : Collections.<TestCaseInfo>emptyList());
        }

        this.outOfDate = csr.getChecksum() != finfo.getChecksum();
        return renderedLines.toArray(new LineRenderInfo[0]);
    }

    private String[] calcCoverageMsg(FullElementInfo lineElement, String emptyCoverageChar) {
        if (lineElement instanceof BranchInfo && !((BranchInfo) lineElement).isInstrumented()) {
            return new String[] { getRegionStartStr(lineElement) + "coverage not measured due to assignment in expression.", "?" };
        } else {
            return new String[] {
                lineElement.getConstruct().calcCoverageMsg(
                    lineElement,
                    lineElement.getHitCount(),
                    lineElement instanceof BranchInfo ? ((BranchInfo) lineElement).getFalseHitCount() : 0,
                    Locale.US),
                emptyCoverageChar
            };
        }
    }

    private boolean noHits(ElementInfo lineElement) {
        return lineElement instanceof BranchInfo
            ? (((BranchInfo)lineElement).getTrueHitCount() == 0 || ((BranchInfo)lineElement).getFalseHitCount() == 0)
            : lineElement.getHitCount() == 0;
    }

    private int hitCounts(ElementInfo lineElement) {
        if (lineElement instanceof BranchInfo) {
            int tc = ((BranchInfo)lineElement).getTrueHitCount();
            int fc = ((BranchInfo)lineElement).getFalseHitCount();
            int hits = (tc == Integer.MAX_VALUE || fc == Integer.MAX_VALUE) ? Integer.MAX_VALUE : tc + fc;
            return hits < 0 ? Integer.MAX_VALUE : hits;
        } else {
            return lineElement.getHitCount();
        }
    }

    private ChecksummingReader render(final FullFileInfo finfo, final List<LineRenderInfo> renderedLines,
                                      final String emptyCoverageMsg, final SourceRenderer renderer) throws Exception {
        Logger.getInstance().debug("Rendering " + finfo.getName() + " with renderer " + renderer.getClass().getName());
        ChecksummingReader csr;
        csr = getChecksummingReader(finfo);
        try {
            renderer.render(renderedLines, csr, finfo, renderingHelper, emptyCoverageMsg, tabStr, spaceChar);
        } finally {
            csr.close();
        }
        return csr;
    }

    private String extensionOf(String path) {
        return path.substring(Math.max(0, path.lastIndexOf('.')), path.length());
    }

    public static List<String> getSrcLines(FileInfo finfo) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(finfo.getSourceReader());
            List<String> srclines = newArrayList();
            String line;
            while ((line = reader.readLine()) != null) {
                srclines.add(line);
            }
            return srclines;
        } finally {
            IOStreamUtils.close(reader);
        }
    }

    private static ChecksummingReader getChecksummingReader(FullFileInfo finfo) throws IOException {
        return new ChecksummingReader(finfo.getSourceReader());
    }

    public static String getRegionStartStr(SourceInfo region) {
        return String.format("Line %d, Col %d: ", region.getStartLine(), region.getStartColumn());
    }
}
