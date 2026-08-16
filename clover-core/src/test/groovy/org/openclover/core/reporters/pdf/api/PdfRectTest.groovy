package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

/**
 * PDF user space has its origin at the bottom-left of the page with the y axis growing upwards,
 * which is the opposite of the AWT convention the widgets are otherwise written against.
 */
class PdfRectTest extends TestCase {

    void testEdgesAreDerivedFromTheBottomLeftCorner() {
        PdfRect rect = new PdfRect(10d, 20d, 100d, 50d)

        assertEquals(10d, rect.x, 0.001d)
        assertEquals(20d, rect.y, 0.001d)
        assertEquals(100d, rect.width, 0.001d)
        assertEquals(50d, rect.height, 0.001d)
        assertEquals("the top edge is above the origin", 70d, rect.top, 0.001d)
        assertEquals(110d, rect.right, 0.001d)
    }

    void testAZeroSizedRectangleCollapsesOntoItsCorner() {
        PdfRect rect = new PdfRect(5d, 6d, 0d, 0d)

        assertEquals(5d, rect.right, 0.001d)
        assertEquals(6d, rect.top, 0.001d)
    }

    void testMarginsAreKeptAsGiven() {
        PdfMargins margins = new PdfMargins(1d, 2d, 3d, 4d)

        assertEquals(1d, margins.left, 0.001d)
        assertEquals(2d, margins.right, 0.001d)
        assertEquals(3d, margins.top, 0.001d)
        assertEquals(4d, margins.bottom, 0.001d)
    }

    void testPageSizesAreInPostScriptPoints() {
        assertEquals(595d, PdfPageSize.A4.width, 0.001d)
        assertEquals(842d, PdfPageSize.A4.height, 0.001d)
        assertEquals(612d, PdfPageSize.LETTER.width, 0.001d)
        assertEquals(792d, PdfPageSize.LETTER.height, 0.001d)
    }
}
