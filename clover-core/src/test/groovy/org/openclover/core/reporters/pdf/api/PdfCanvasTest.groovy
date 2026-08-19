package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.RecordingCanvas

import java.awt.Graphics2D

/**
 * The scoped drawing call every widget paints through: an AWT context is opened, handed over and
 * committed to the page, without the caller having to remember to close anything.
 */
class PdfCanvasTest extends TestCase {

    private RecordingCanvas canvas

    void setUp() {
        canvas = new RecordingCanvas()
    }

    void testTheActionIsGivenAContextMappedOntoTheRectangle() {
        PdfRect bounds = new PdfRect(10d, 20d, 300d, 100d)
        Graphics2D handedOver = null

        canvas.inGraphicsScope(bounds) { graphics -> handedOver = graphics }

        assertEquals(1, canvas.graphicsScopes.size())
        assertSame(bounds, canvas.graphicsScopes[0].bounds)
        assertSame(canvas.graphicsScopes[0].graphics, handedOver)
    }

    void testTheScopeIsCommittedOnceTheActionReturns() {
        canvas.inGraphicsScope(new PdfRect(0d, 0d, 10d, 10d)) { graphics -> }

        assertTrue(canvas.graphicsScopes[0].closed)
    }

    /**
     * The drawing is only stamped onto the page when the scope closes, so a painting action that
     * fails must not leave the scope open - which is the point of the call taking the action.
     */
    void testTheScopeIsCommittedEvenWhenTheActionThrows() {
        try {
            canvas.inGraphicsScope(new PdfRect(0d, 0d, 10d, 10d)) { graphics ->
                throw new IllegalStateException("painting failed")
            }
            fail("expected the failure to propagate")
        } catch (IllegalStateException expected) {
            assertEquals("painting failed", expected.message)
        }

        assertTrue("the scope must not be left open", canvas.graphicsScopes[0].closed)
    }
}
