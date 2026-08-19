package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

class PdfBorderTest extends TestCase {

    void testOfCollectsTheGivenEdges() {
        Set<PdfBorder> borders = PdfBorder.of(PdfBorder.TOP, PdfBorder.BOTTOM)

        assertTrue(borders.contains(PdfBorder.TOP))
        assertTrue(borders.contains(PdfBorder.BOTTOM))
        assertFalse(borders.contains(PdfBorder.LEFT))
        assertFalse(borders.contains(PdfBorder.RIGHT))
    }

    void testBoxHasEveryEdge() {
        PdfBorder.values().each {
            assertTrue(PdfBorder.BOX.contains(it))
        }
    }

    void testNoneIsEmpty() {
        assertTrue(PdfBorder.NONE.isEmpty())
        assertTrue(PdfBorder.of().isEmpty())
    }

    /**
     * Border sets are handed out to cells and shared between them, so none of them may be
     * modifiable from the outside.
     */
    void testBorderSetsAreImmutable() {
        [PdfBorder.NONE, PdfBorder.BOX, PdfBorder.of(PdfBorder.TOP)].each { borders ->
            try {
                borders.add(PdfBorder.LEFT)
                fail("expected ${borders} to be immutable")
            } catch (UnsupportedOperationException expected) {
                // as intended
            }
        }
    }
}
