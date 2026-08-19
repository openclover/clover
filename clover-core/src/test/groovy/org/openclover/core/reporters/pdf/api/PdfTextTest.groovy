package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

import java.awt.Color

class PdfTextTest extends TestCase {

    private static final PdfFontSpec FONT = PdfFontSpec.sans(10)

    void testRunsAreKeptInTheOrderTheyWereAdded() {
        PdfText text = PdfText.builder()
                .add("one ", FONT)
                .add("two ", FONT)
                .addLink("three", FONT, "https://openclover.org")
                .build()

        assertEquals(["one ", "two ", "three"], text.runs.collect { it.text })
    }

    void testOnlyLinkRunsCarryAnAnchor() {
        PdfText text = PdfText.builder()
                .add("plain", FONT)
                .addLink("link", FONT, "https://openclover.org")
                .build()

        assertNull(text.runs[0].anchor)
        assertEquals("https://openclover.org", text.runs[1].anchor)
    }

    void testAppendingAnotherTextTakesOverItsRuns() {
        PdfText second = PdfText.builder().add("b", FONT).add("c", FONT).build()
        PdfText first = PdfText.builder().add("a", FONT).add(second).build()

        assertEquals(["a", "b", "c"], first.runs.collect { it.text })
        assertEquals("the appended text is left alone", 2, second.runs.size())
    }

    /**
     * The point of the builder: a text handed to a cell keeps the runs it had at that moment,
     * however much the builder that produced it goes on to be used.
     */
    void testABuiltTextIsUnaffectedByFurtherBuilding() {
        PdfTextBuilder builder = PdfText.builder().add("first", FONT)
        PdfText snapshot = builder.build()

        builder.add(" second", FONT)

        assertEquals(["first"], snapshot.runs.collect { it.text })
        assertEquals(["first", " second"], builder.build().runs.collect { it.text })
    }

    void testToBuilderDerivesALongerTextWithoutTouchingTheOriginal() {
        PdfText original = PdfText.of("head", FONT)

        PdfText derived = original.toBuilder().add(" tail", FONT).build()

        assertEquals(["head"], original.runs.collect { it.text })
        assertEquals(["head", " tail"], derived.runs.collect { it.text })
    }

    void testSingleRunFactories() {
        assertEquals(["only"], PdfText.of("only", FONT).runs.collect { it.text })

        PdfText link = PdfText.ofLink("click", FONT, "https://openclover.org")
        assertEquals(1, link.runs.size())
        assertEquals("https://openclover.org", link.runs[0].anchor)
    }

    void testIsEmptyOnlyWhenNoRunCarriesAnyText() {
        assertTrue(PdfText.builder().build().isEmpty())
        assertTrue(PdfText.of("", FONT).isEmpty())
        assertTrue(PdfText.builder().add("", FONT).add("", FONT).build().isEmpty())
        assertFalse(PdfText.of(" ", FONT).isEmpty())
        assertFalse(PdfText.builder().add("", FONT).add("x", FONT).build().isEmpty())
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
     * Text measures itself through the layout it is handed, passing on the leading and alignment
     * its cell was styled with.
     */
    void testTextMeasuresItselfThroughTheLayout() {
        PdfText text = PdfText.of("hello", FONT)
        PdfCellStyle style = PdfCellStyle.builder()
                .setLeading(2d, 0.9d)
                .setHorizontalAlignment(PdfAlign.Horizontal.RIGHT)
                .build()
        RecordingLayout layout = new RecordingLayout()

        PdfMeasuredContent measured = text.measure(layout, style, 30d)

        assertSame(text, layout.measuredText)
        assertEquals(30d, layout.measuredWidth, 0.001d)
        assertEquals(PdfAlign.Horizontal.RIGHT, layout.measuredAlignment)
        assertEquals(2d, layout.measuredFixedLeading, 0.001d)
        assertEquals(0.9d, layout.measuredMultipliedLeading, 0.001d)
        assertEquals(RecordingLayout.TEXT_HEIGHT, measured.height, 0.001d)
    }

    /** Records what content asked of it, so delegation can be asserted without a real engine. */
    private static class RecordingLayout implements PdfLayout {

        static final double TEXT_HEIGHT = 17d

        PdfText measuredText
        double measuredWidth, measuredFixedLeading, measuredMultipliedLeading
        PdfAlign.Horizontal measuredAlignment

        @Override
        PdfMeasuredContent measureText(PdfText text, double width, PdfAlign.Horizontal alignment,
                                       double fixedLeading, double multipliedLeading) {
            measuredText = text
            measuredWidth = width
            measuredAlignment = alignment
            measuredFixedLeading = fixedLeading
            measuredMultipliedLeading = multipliedLeading
            return new PdfMeasuredContent() {

                @Override
                double getHeight() {
                    return TEXT_HEIGHT
                }

                @Override
                void draw(PdfCanvas canvas, PdfRect bounds) {
                    throw new UnsupportedOperationException()
                }
            }
        }

        @Override
        PdfMeasuredContent measureTable(PdfTable table, double availableWidth) {
            throw new UnsupportedOperationException()
        }
    }
}
