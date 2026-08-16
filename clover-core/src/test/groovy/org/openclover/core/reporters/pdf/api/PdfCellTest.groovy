package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

class PdfCellTest extends TestCase {

    private static PdfCell cellWithPadding(double padding) {
        return new PdfCell(PdfCellStyle.builder().setPadding(padding).build(), null)
    }

    void testContentAreaHasThePaddingTakenOff() {
        PdfCell cell = cellWithPadding(3d)

        assertEquals(100d - 6d, cell.contentWidth(100d), 0.001d)
        assertEquals(50d - 6d, cell.contentHeight(50d), 0.001d)
    }

    /**
     * A cell narrower than its own padding must not hand out a negative content area, which would
     * turn into a negative wrapping width further down.
     */
    void testContentAreaIsNeverNegative() {
        PdfCell cell = cellWithPadding(10d)

        assertEquals(0d, cell.contentWidth(4d), 0.001d)
        assertEquals(0d, cell.contentHeight(4d), 0.001d)
    }

    void testAsymmetricPaddingIsAccountedForOnBothEdges() {
        PdfCellStyle style = PdfCellStyle.builder()
                .setPaddingLeft(1d).setPaddingRight(2d)
                .setPaddingTop(3d).setPaddingBottom(4d)
                .build()
        PdfCell cell = new PdfCell(style, null)

        assertEquals(100d - 3d, cell.contentWidth(100d), 0.001d)
        assertEquals(100d - 7d, cell.contentHeight(100d), 0.001d)
    }

    void testColspanComesFromTheStyle() {
        PdfCell cell = new PdfCell(PdfCellStyle.builder().setColspan(4).build(), null)

        assertEquals(4, cell.colspan)
    }

    void testAnEmptyCellHasNoContent() {
        assertNull(new PdfCell(PdfCellStyle.builder().build(), null).content)
    }

    void testContentIsKeptAsGiven() {
        PdfText text = PdfText.of("hello", PdfFontSpec.sans(10))
        PdfCell cell = new PdfCell(PdfCellStyle.builder().build(), text)

        assertSame(text, cell.content)
    }
}
