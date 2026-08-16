package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

/**
 * Images bundled with the reports, embedded into a document once each. The footer logo is drawn on
 * every page, so this is what keeps a long report from carrying one copy of it per page.
 */
class ImageRegistryTest extends TestCase {

    private static final String LOGO = "pdf_res/logo1.png"

    private PDDocument document
    private ImageRegistry images

    void setUp() {
        document = new PDDocument()
        images = new ImageRegistry(document)
    }

    void tearDown() {
        document.close()
    }

    void testABundledImageIsLoadedFromTheClasspath() {
        PDImageXObject logo = images.get(LOGO)

        assertNotNull(logo)
        assertTrue("expected a real image, got ${logo.width}x${logo.height}", logo.width > 0)
        assertTrue(logo.height > 0)
    }

    void testTheSameImageIsEmbeddedOnlyOnce() {
        PDImageXObject first = images.get(LOGO)
        PDImageXObject second = images.get(LOGO)

        assertSame(first, second)
        assertEquals(1, images.size())
    }

    /**
     * A bundled report resource that is not on the classpath is a packaging fault, not something
     * to silently render around.
     */
    void testAMissingImageFailsLoudly() {
        try {
            images.get("pdf_res/there-is-no-such-image.png")
            fail("expected a missing image to fail the report")
        } catch (IllegalStateException expected) {
            assertTrue(expected.message, expected.message.contains("there-is-no-such-image.png"))
        }
        assertEquals("nothing should have been cached", 0, images.size())
    }
}
