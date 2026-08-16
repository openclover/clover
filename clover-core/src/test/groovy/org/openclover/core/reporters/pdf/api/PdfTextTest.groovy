package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.RecordingCanvas

import java.awt.Color

class PdfTextTest extends TestCase {

    private static final PdfFontSpec FONT = PdfFontSpec.sans(10)

    void testRunsAreKeptInTheOrderTheyWereAdded() {
        PdfText text = PdfText.of("one ", FONT)
                .add("two ", FONT)
                .addLink("three", FONT, "https://openclover.org")

        assertEquals(["one ", "two ", "three"], text.runs.collect { it.text })
    }

    void testOnlyLinkRunsCarryAnAnchor() {
        PdfText text = PdfText.of("plain", FONT).addLink("link", FONT, "https://openclover.org")

        assertNull(text.runs[0].anchor)
        assertEquals("https://openclover.org", text.runs[1].anchor)
    }

    void testAppendingAnotherTextTakesOverItsRuns() {
        PdfText first = PdfText.of("a", FONT)
        PdfText second = PdfText.of("b", FONT).add("c", FONT)

        first.add(second)

        assertEquals(["a", "b", "c"], first.runs.collect { it.text })
        assertEquals("the appended text is left alone", 2, second.runs.size())
    }

    void testIsEmptyOnlyWhenNoRunCarriesAnyText() {
        assertTrue(new PdfText().isEmpty())
        assertTrue(PdfText.of("", FONT).isEmpty())
        assertTrue(PdfText.of("", FONT).add("", FONT).isEmpty())
        assertFalse(PdfText.of(" ", FONT).isEmpty())
        assertFalse(PdfText.of("", FONT).add("x", FONT).isEmpty())
    }

    void testRunsAreNotModifiableFromTheOutside() {
        PdfText text = PdfText.of("a", FONT)

        try {
            text.runs.add(new PdfTextRun("b", FONT))
            fail("expected the run list to be immutable")
        } catch (UnsupportedOperationException expected) {
            // as intended
        }
    }

    /**
     * A null run reads as empty rather than blowing up when the text is measured - report titles
     * and package names are not guaranteed to be set.
     */
    void testNullRunTextReadsAsEmpty() {
        assertEquals("", new PdfTextRun(null, FONT).text)
    }

    void testFontSpecFactoriesDefaultToBlackRegularSans() {
        PdfFontSpec plain = PdfFontSpec.sans(8d)

        assertEquals(PdfFontFamily.SANS, plain.family)
        assertEquals(PdfFontStyle.REGULAR, plain.style)
        assertEquals(Color.black, plain.colour)
        assertEquals(8d, plain.size, 0.001d)

        assertEquals(PdfFontStyle.BOLD, PdfFontSpec.sans(8d, PdfFontStyle.BOLD).style)
        assertEquals(Color.RED, PdfFontSpec.sans(8d, PdfFontStyle.ITALIC, Color.RED).colour)
    }

    /**
     * Text measures and draws itself through the layout it is handed, passing on the leading and
     * alignment its cell was styled with.
     */
    void testTextDelegatesToTheLayout() {
        PdfText text = PdfText.of("hello", FONT)
        PdfCellStyle style = PdfCellStyle.builder()
                .setLeading(2d, 0.9d)
                .setHorizontalAlignment(PdfAlign.Horizontal.RIGHT)
                .build()
        RecordingLayout layout = new RecordingLayout()
        RecordingCanvas canvas = new RecordingCanvas()
        PdfRect bounds = new PdfRect(1d, 2d, 30d, 40d)

        assertEquals(RecordingLayout.TEXT_HEIGHT, text.height(layout, style, 30d), 0.001d)
        text.draw(layout, canvas, style, bounds)

        assertEquals(30d, layout.measuredWidth, 0.001d)
        assertEquals(2d, layout.measuredFixedLeading, 0.001d)
        assertEquals(0.9d, layout.measuredMultipliedLeading, 0.001d)

        assertSame(text, layout.drawnText)
        assertSame(bounds, layout.drawnBounds)
        assertEquals(PdfAlign.Horizontal.RIGHT, layout.drawnAlignment)
        assertEquals(2d, layout.drawnFixedLeading, 0.001d)
        assertEquals(0.9d, layout.drawnMultipliedLeading, 0.001d)
    }

    /** Records what content asked of it, so delegation can be asserted without a real engine. */
    private static class RecordingLayout implements PdfLayout {

        static final double TEXT_HEIGHT = 17d

        double measuredWidth, measuredFixedLeading, measuredMultipliedLeading
        PdfText drawnText
        PdfRect drawnBounds
        PdfAlign.Horizontal drawnAlignment
        double drawnFixedLeading, drawnMultipliedLeading

        @Override
        double textHeight(PdfText text, double width, double fixedLeading, double multipliedLeading) {
            measuredWidth = width
            measuredFixedLeading = fixedLeading
            measuredMultipliedLeading = multipliedLeading
            return TEXT_HEIGHT
        }

        @Override
        void drawText(PdfCanvas canvas, PdfText text, PdfRect bounds,
                      PdfAlign.Horizontal alignment, double fixedLeading, double multipliedLeading) {
            drawnText = text
            drawnBounds = bounds
            drawnAlignment = alignment
            drawnFixedLeading = fixedLeading
            drawnMultipliedLeading = multipliedLeading
        }

        @Override
        double nestedTableHeight(PdfTable table, double availableWidth) {
            throw new UnsupportedOperationException()
        }

        @Override
        void drawNestedTable(PdfCanvas canvas, PdfTable table, double x, double topY,
                             double availableWidth) {
            throw new UnsupportedOperationException()
        }
    }
}
