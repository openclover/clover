package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

class PdfBorderTest extends TestCase {

    void testHasDetectsSingleEdges() {
        int borders = PdfBorder.TOP | PdfBorder.BOTTOM

        assertTrue(PdfBorder.has(borders, PdfBorder.TOP))
        assertTrue(PdfBorder.has(borders, PdfBorder.BOTTOM))
        assertFalse(PdfBorder.has(borders, PdfBorder.LEFT))
        assertFalse(PdfBorder.has(borders, PdfBorder.RIGHT))
    }

    void testBoxHasEveryEdge() {
        [PdfBorder.TOP, PdfBorder.BOTTOM, PdfBorder.LEFT, PdfBorder.RIGHT].each {
            assertTrue(PdfBorder.has(PdfBorder.BOX, it))
        }
    }

    /**
     * NONE is the empty mask, so asking whether it is "present" has no meaningful answer - it
     * must be rejected rather than silently answering false for every input, including for a
     * cell that genuinely has no border.
     */
    void testHasRejectsNone() {
        try {
            PdfBorder.has(PdfBorder.BOTTOM, PdfBorder.NONE)
            fail("expected NONE to be rejected as an edge")
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message, expected.message.contains("isNone"))
        }
    }

    /**
     * A combination asks whether every listed edge is present, not merely one of them.
     */
    void testHasAcceptsCombinedEdges() {
        assertTrue(PdfBorder.has(PdfBorder.BOX, PdfBorder.TOP | PdfBorder.LEFT))
        assertTrue(PdfBorder.has(PdfBorder.TOP | PdfBorder.LEFT, PdfBorder.TOP | PdfBorder.LEFT))
        assertFalse(PdfBorder.has(PdfBorder.TOP, PdfBorder.TOP | PdfBorder.LEFT))
        assertFalse(PdfBorder.has(PdfBorder.TOP | PdfBorder.BOTTOM, PdfBorder.TOP | PdfBorder.LEFT))
    }

    void testIsNone() {
        assertTrue(PdfBorder.isNone(PdfBorder.NONE))
        assertFalse(PdfBorder.isNone(PdfBorder.BOX))
        assertFalse(PdfBorder.isNone(PdfBorder.TOP))
    }
}
