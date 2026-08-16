package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.api.PdfCell
import org.openclover.core.reporters.pdf.api.PdfCellStyle

/**
 * The measured form of a table, which both the page flow and the renderer read: the flow to decide
 * where a page breaks, the renderer to paint.
 */
class TableLayoutTest extends TestCase {

    private static TableLayout.Row row(double height) {
        return new TableLayout.Row([new PdfCell(PdfCellStyle.builder().build(), null)], height)
    }

    void testHeightIsTheSumOfTheRowHeights() {
        TableLayout layout = new TableLayout([10d, 20d] as double[], [row(5d), row(7d), row(11d)])

        assertEquals(23d, layout.height, 0.001d)
    }

    void testAnEmptyTableHasNoHeight() {
        assertEquals(0d, new TableLayout([10d] as double[], []).height, 0.001d)
    }

    void testColumnWidthsAndRowsAreReportedAsMeasured() {
        TableLayout.Row single = row(9d)
        TableLayout layout = new TableLayout([30d, 70d] as double[], [single])

        assertEquals(30d, layout.columnWidths[0], 0.001d)
        assertEquals(70d, layout.columnWidths[1], 0.001d)
        assertEquals(1, layout.rows.size())
        assertSame(single, layout.rows[0])
        assertEquals(9d, single.height, 0.001d)
        assertEquals(1, single.cells.size())
    }

    void testRowsAreNotModifiableFromTheOutside() {
        TableLayout layout = new TableLayout([10d] as double[], [row(1d)])

        try {
            layout.rows.add(row(2d))
            fail("expected the measured rows to be immutable")
        } catch (UnsupportedOperationException expected) {
            // as intended
        }
    }
}
