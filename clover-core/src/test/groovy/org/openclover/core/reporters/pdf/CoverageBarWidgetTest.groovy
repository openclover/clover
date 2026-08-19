package org.openclover.core.reporters.pdf

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.api.PdfRect

/**
 * The covered/uncovered bar shown in the last column of the coverage tables.
 */
class CoverageBarWidgetTest extends TestCase {

    private static final double FONT_HEIGHT = 10d
    private static final PDFColours COLOURS = PDFColours.COL_COLOURS
    private static final PdfRect BOUNDS = new PdfRect(100d, 200d, 200d, 20d)

    private RecordingCanvas canvas

    void setUp() {
        canvas = new RecordingCanvas()
    }

    private static CoverageBarWidget bar(double coveredPc, double paddingRatio = 0d) {
        return new CoverageBarWidget(coveredPc, FONT_HEIGHT, paddingRatio, COLOURS)
    }

    void testTheBarIsDrawnSlightlyShorterThanItsFont() {
        assertEquals(FONT_HEIGHT - 2d, bar(0.5d).preferredHeight(), 0.001d)
    }

    /**
     * The covered part of the bar is as wide a fraction of the bar as the coverage it reports.
     */
    void testCoveredWidthFollowsTheCoverage() {
        bar(0.25d).draw(canvas, BOUNDS)

        // the full bar is filled with the uncovered colour first, then the covered part over it
        List<RecordingCanvas.DrawnRect> filled = canvas.filled()
        assertEquals(2, filled.size())
        assertEquals(COLOURS.COL_BAR_UNCOVERED, filled[0].colour)
        assertEquals(200d, filled[0].rect.width, 0.001d)
        assertEquals(COLOURS.COL_BAR_COVERED, filled[1].colour)
        assertEquals(50d, filled[1].rect.width, 0.001d)
        assertEquals(filled[0].rect.x, filled[1].rect.x, 0.001d)
    }

    void testFullCoverageFillsTheWholeBar() {
        bar(1d).draw(canvas, BOUNDS)

        assertEquals(200d, canvas.filled()[1].rect.width, 0.001d)
    }

    /**
     * A metric that overshoots must not paint outside the bar.
     */
    void testCoverageAboveOneIsClamped() {
        bar(1.5d).draw(canvas, BOUNDS)

        assertEquals(200d, canvas.filled()[1].rect.width, 0.001d)
    }

    /**
     * A negative coverage is the "nothing to report" sentinel: the bar is drawn in one flat
     * colour instead of being split.
     */
    void testNothingToReportIsDrawnAsASingleFlatBar() {
        bar(-1d).draw(canvas, BOUNDS)

        List<RecordingCanvas.DrawnRect> filled = canvas.filled()
        assertEquals(1, filled.size())
        assertEquals(COLOURS.COL_BAR_NA, filled[0].colour)
        assertEquals(200d, filled[0].rect.width, 0.001d)
    }

    void testZeroCoverageStillDrawsTheUncoveredBar() {
        bar(0d).draw(canvas, BOUNDS)

        List<RecordingCanvas.DrawnRect> filled = canvas.filled()
        assertEquals(COLOURS.COL_BAR_UNCOVERED, filled[0].colour)
        assertEquals(0d, filled[1].rect.width, 0.001d)
    }

    void testTheBarIsOutlinedAndVerticallyCentred() {
        bar(0.5d).draw(canvas, BOUNDS)

        RecordingCanvas.DrawnRect outline = canvas.stroked().last()
        assertEquals(COLOURS.COL_BAR_BORDER, outline.colour)
        assertEquals(200d, outline.rect.width, 0.001d)
        assertEquals(FONT_HEIGHT - 2d, outline.rect.height, 0.001d)
        // equal gaps above and below within the 20pt cell
        assertEquals(200d + (20d - 8d) / 2, outline.rect.y, 0.001d)
    }

    void testPaddingIsTakenOffBothSides() {
        bar(0.5d, 0.1d).draw(canvas, BOUNDS)

        RecordingCanvas.DrawnRect fullBar = canvas.filled()[0]
        assertEquals(200d - 2 * 20d, fullBar.rect.width, 0.001d)
        assertEquals(100d + 20d, fullBar.rect.x, 0.001d)
    }
}
