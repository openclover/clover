package org.openclover.core.reporters.pdf

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.api.PdfAlign
import org.openclover.core.reporters.pdf.api.PdfRect

/**
 * The bar shown next to a class in the "top movers" and "classes added" tables. A gain grows the
 * bar from the left with its label after it; a loss mirrors that, so the two halves of the table
 * meet in the middle.
 */
class CoverageDiffBarWidgetTest extends TestCase {

    private static final double FONT_SIZE = 8d
    private static final PDFColours COLOURS = PDFColours.COL_COLOURS
    private static final PdfRect BOUNDS = new PdfRect(0d, 100d, 100d, 20d)

    private RecordingCanvas canvas

    void setUp() {
        canvas = new RecordingCanvas()
    }

    private static CoverageDiffBarWidget diffBar(double pcDiff, double pcNow = 0.5d) {
        return new CoverageDiffBarWidget(pcDiff, pcNow, FONT_SIZE, COLOURS)
    }

    void testHeightLeavesRoomForTheRowPaddingItReplaces() {
        assertEquals(FONT_SIZE + 4d, diffBar(10d).preferredHeight(), 0.001d)
    }

    void testAGainGrowsFromTheLeftWithItsLabelAfterIt() {
        diffBar(25d).draw(canvas, BOUNDS)

        RecordingCanvas.DrawnRect bar = canvas.filled()[0]
        assertEquals(COLOURS.COL_BAR_COVERED, bar.colour)
        assertEquals(0d, bar.rect.x, 0.001d)
        // the variable part of the width is 80% of the cell, scaled by the change
        assertEquals(80d * 0.25d, bar.rect.width, 0.001d)

        RecordingCanvas.DrawnText label = canvas.texts[0]
        assertEquals(PdfAlign.Horizontal.LEFT, label.horizontal)
        assertEquals(bar.rect.right, label.bounds.x, 0.001d)
    }

    void testALossGrowsFromTheRightWithItsLabelBeforeIt() {
        diffBar(-25d).draw(canvas, BOUNDS)

        RecordingCanvas.DrawnRect bar = canvas.filled()[0]
        assertEquals(COLOURS.COL_BAR_UNCOVERED, bar.colour)
        assertEquals("a loss reaches the right edge", 100d, bar.rect.right, 0.001d)
        // 20% base plus the scaled remainder is given to the label, the rest to the bar
        assertEquals(100d - (20d + 80d * 0.75d), bar.rect.width, 0.001d)

        RecordingCanvas.DrawnText label = canvas.texts[0]
        assertEquals(PdfAlign.Horizontal.RIGHT, label.horizontal)
        assertEquals(0d, label.bounds.x, 0.001d)
        assertEquals(bar.rect.x, label.bounds.right, 0.001d)
    }

    void testABiggerGainMakesALongerBar() {
        diffBar(10d).draw(canvas, BOUNDS)
        double small = canvas.filled()[0].rect.width

        canvas = new RecordingCanvas()
        diffBar(50d).draw(canvas, BOUNDS)

        assertTrue(canvas.filled()[0].rect.width > small)
    }

    void testTheBarAndTheLabelTogetherFillTheCell() {
        diffBar(30d).draw(canvas, BOUNDS)

        RecordingCanvas.DrawnRect bar = canvas.filled()[0]
        RecordingCanvas.DrawnText label = canvas.texts[0]

        assertEquals(BOUNDS.width, bar.rect.width + label.bounds.width, 0.001d)
    }

    void testTheBarIsOutlinedAndVerticallyCentred() {
        diffBar(30d).draw(canvas, BOUNDS)

        RecordingCanvas.DrawnRect outline = canvas.stroked()[0]
        assertEquals(COLOURS.COL_BAR_BORDER, outline.colour)
        assertEquals(FONT_SIZE - 2d, outline.rect.height, 0.001d)
        assertEquals(100d + (20d - (FONT_SIZE - 2d)) / 2, outline.rect.y, 0.001d)
    }

    /**
     * A gain is labelled with the change first, a loss with the resulting coverage first, so that
     * in both cases the number nearest the bar is the one it encodes.
     */
    void testTheLabelReadsFromTheBarOutwards() {
        diffBar(25d, 0.5d).draw(canvas, BOUNDS)
        String gain = canvas.texts[0].plainText()

        canvas = new RecordingCanvas()
        diffBar(-25d, 0.5d).draw(canvas, BOUNDS)
        String loss = canvas.texts[0].plainText()

        assertTrue("expected a signed change first, got: " + gain, gain.startsWith("+"))
        assertTrue("expected the coverage in brackets, got: " + gain, gain.contains("("))
        assertTrue("expected the coverage first, got: " + loss, loss.startsWith("("))
        assertTrue("expected a negative change, got: " + loss, loss.contains("-"))
    }
}
