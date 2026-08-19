package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

import java.awt.Color

/**
 * Cell grouping and cell styling: how cells wrap into rows, and how each one gets the style it is
 * drawn with.
 */
class PdfTableCellsTest extends TestCase {

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
}
