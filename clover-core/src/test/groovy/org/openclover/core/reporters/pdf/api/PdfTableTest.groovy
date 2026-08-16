package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

import java.awt.Color

/**
 * Cell grouping and the width model, both of which the layout engine reads before it draws
 * anything.
 */
class PdfTableTest extends TestCase {

    private static final PdfFontSpec FONT = PdfFontSpec.sans(10)

    private static PdfText text(String value) {
        return PdfText.of(value, FONT)
    }

    void testATableNeedsAtLeastOneColumn() {
        try {
            new PdfTable(0)
            fail("expected a table without columns to be rejected")
        } catch (IllegalArgumentException expected) {
            // as intended
        }
    }

    void testCellsWrapIntoRowsOnceTheColumnsAreFull() {
        PdfTable table = new PdfTable(2)
        4.times { table.addCell(text("cell ${it}")) }

        List<List<PdfCell>> rows = table.rows

        assertEquals(2, rows.size())
        rows.each { assertEquals(2, it.size()) }
    }

    /**
     * A spanned cell fills several columns at once, so the row wraps sooner.
     */
    void testColspanFillsSeveralColumns() {
        PdfTable table = new PdfTable(3)
        table.defaultStyle.setColspan(2)
        table.addCell(text("wide"))
        table.defaultStyle.setColspan(1)
        table.addCell(text("narrow"))
        table.addCell(text("next row"))

        List<List<PdfCell>> rows = table.rows

        assertEquals(2, rows.size())
        assertEquals(2, rows[0].size())
        assertEquals(1, rows[1].size())
    }

    /**
     * A trailing partial row is kept; the layout engine treats the missing columns as empty.
     */
    void testTrailingPartialRowIsKept() {
        PdfTable table = new PdfTable(3)
        table.addCell(text("a"))
        table.addCell(text("b"))

        assertEquals(1, table.rows.size())
        assertEquals(2, table.rows[0].size())
    }

    /**
     * A colspan wider than the table must still terminate the row rather than swallowing every
     * cell that follows.
     */
    void testColspanWiderThanTheTableStillEndsTheRow() {
        PdfTable table = new PdfTable(2)
        table.defaultStyle.setColspan(5)
        table.addCell(text("very wide"))
        table.addCell(text("second row"))

        assertEquals(2, table.rows.size())
    }

    /**
     * Rows are cached, so adding a cell afterwards has to invalidate the grouping.
     */
    void testAddingACellInvalidatesTheCachedRows() {
        PdfTable table = new PdfTable(1)
        table.addCell(text("first"))
        assertEquals(1, table.rows.size())

        table.addCell(text("second"))
        assertEquals(2, table.rows.size())
    }

    void testRowsAreNotModifiableFromTheOutside() {
        PdfTable table = new PdfTable(1)
        table.addCell(text("a"))

        try {
            table.rows.add([])
            fail("expected the row grouping to be immutable")
        } catch (UnsupportedOperationException expected) {
            // as intended
        }
    }

    /**
     * The style template is copied into each cell as it is added, so editing it afterwards must
     * not reach back into the cells already there.
     */
    void testCellsSnapshotTheDefaultStyle() {
        PdfTable table = new PdfTable(1)
        table.defaultStyle.setBackgroundColour(Color.RED)
        table.addCell(text("red"))
        table.defaultStyle.setBackgroundColour(Color.GREEN)
        table.addCell(text("green"))

        assertEquals(Color.RED, table.cells[0].style.backgroundColour)
        assertEquals(Color.GREEN, table.cells[1].style.backgroundColour)
    }

    void testAnEmptyCellIsStillACell() {
        PdfTable table = new PdfTable(1)
        table.addCell()

        assertEquals(1, table.cells.size())
        assertNull(table.cells[0].content)
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

    /**
     * The style protocol: a table's default style is configured once, and a cell that deviates
     * says so at the point it is added, without disturbing the cells added after it.
     */
    void testACellCustomiserAppliesToThatCellAlone() {
        PdfTable table = new PdfTable(3)
        table.getDefaultStyle().setHorizontalAlignment(PdfAlign.Horizontal.LEFT).setPadding(2d)

        table.addCell(text("plain"))
        table.addCell(text("centred"), { it.setHorizontalAlignment(PdfAlign.Horizontal.CENTER) })
        table.addCell(text("plain again"))

        assertEquals(PdfAlign.Horizontal.LEFT, table.cells[0].style.horizontalAlignment)
        assertEquals(PdfAlign.Horizontal.CENTER, table.cells[1].style.horizontalAlignment)
        assertEquals("the deviation must not leak forward",
                PdfAlign.Horizontal.LEFT, table.cells[2].style.horizontalAlignment)
        // everything the customiser did not touch still comes from the default
        assertEquals(2d, table.cells[1].style.paddingTop, 0.001d)
    }

    void testAnEmptyCellCanDeviateToo() {
        PdfTable table = new PdfTable(2)

        table.addEmptyCell { it.setColspan(2) }

        assertNull(table.cells[0].content)
        assertEquals(2, table.cells[0].colspan)
    }

    /**
     * Identically styled cells share one style instance rather than holding a copy each.
     */
    void testCellsInTheDefaultStyleShareIt() {
        PdfTable table = new PdfTable(1)
        table.getDefaultStyle().setPadding(3d)

        table.addCell(text("a"))
        table.addCell(text("b"))

        assertSame(table.cells[0].style, table.cells[1].style)
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
}
