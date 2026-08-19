package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase


/**
 * The width model: the proportions the columns divide the table by, and how wide the table itself
 * comes out of the width available to it.
 */
class PdfTableWidthsTest extends TestCase {

    private static final PdfFontSpec FONT = PdfFontSpec.sans(10)

    private static PdfText text(String value) {
        return PdfText.of(value, FONT)
    }

    void testWidthsMustMatchTheColumnCount() {
        try {
            new PdfTable(3).setWidths([50, 50] as int[])
            fail("expected a mismatched column width array to be rejected")
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message, expected.message.contains("3"))
        }
    }

    void testColumnsAreEquallyProportionedUntilWidthsAreGiven() {
        double[] widths = new PdfTable(4).relativeWidths

        assertEquals(4, widths.length)
        widths.each { assertEquals(1d, it, 0.001d) }
    }

    void testWidthsAreCopiedFromTheCallersArray() {
        double[] given = [1d, 2d] as double[]
        PdfTable table = new PdfTable(2).setWidths(given)

        given[0] = 99d

        assertEquals(1d, table.relativeWidths[0], 0.001d)
    }

    void testRelativeWidthsAreReportedAsAnImmutableList() {
        PdfTable table = new PdfTable(2)

        assertEquals([1d, 1d], table.relativeWidths)

        table.setWidths([30d, 70d] as double[])
        assertEquals([30d, 70d], table.relativeWidths)

        try {
            table.relativeWidths.set(0, 99d)
            fail("expected the column proportions to be immutable")
        } catch (UnsupportedOperationException expected) {
            // as intended
        }
    }

    void testAutoWidthFillsACellButTakesTheDefaultShareOfAPage() {
        PdfTable table = new PdfTable(1)

        assertEquals(PdfTable.WidthMode.AUTO, table.widthMode)
        assertEquals(100d, table.resolveWidth(100d, true), 0.001d)
        assertEquals(PdfTable.DEFAULT_WIDTH_PERCENTAGE, table.resolveWidth(100d, false), 0.001d)
    }

    void testPercentageWidthAppliesWhetherNestedOrNot() {
        PdfTable table = new PdfTable(1).setWidthPercentage(25d)

        assertEquals(PdfTable.WidthMode.PERCENTAGE, table.widthMode)
        assertEquals(25d, table.resolveWidth(100d, false), 0.001d)
        assertEquals(25d, table.resolveWidth(100d, true), 0.001d)
    }

    void testAbsoluteWidthIgnoresWhatIsAvailable() {
        PdfTable table = new PdfTable(1).setTotalWidth(123d)

        assertEquals(PdfTable.WidthMode.ABSOLUTE, table.widthMode)
        assertEquals(123d, table.resolveWidth(100d, false), 0.001d)
        assertEquals(123d, table.resolveWidth(10d, true), 0.001d)
    }

    /**
     * The last width asked for wins, so that a table can be re-pinned without carrying a stale
     * mode around.
     */
    void testTheMostRecentlySetWidthWins() {
        PdfTable table = new PdfTable(1).setTotalWidth(123d).setWidthPercentage(50d)
        assertEquals(50d, table.resolveWidth(100d, false), 0.001d)

        table.setTotalWidth(60d)
        assertEquals(60d, table.resolveWidth(100d, false), 0.001d)
    }

    /**
     * A table of no width is not a table that draws nothing: every word wraps onto its own line,
     * so the row grows without bound while nothing of it is visible.
     */
    void testWidthsMustBePositive() {
        [{ new PdfTable(1).setTotalWidth(-1d) },
         { new PdfTable(1).setTotalWidth(0d) },
         { new PdfTable(1).setWidthPercentage(-1d) },
         { new PdfTable(1).setWidthPercentage(0d) }].each { attempt ->
            try {
                attempt()
                fail("expected a non-positive width to be rejected")
            } catch (IllegalArgumentException expected) {
                // as intended
            }
        }
    }
}
