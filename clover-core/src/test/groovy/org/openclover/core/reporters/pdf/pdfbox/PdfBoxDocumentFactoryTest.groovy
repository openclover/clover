package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.api.PdfDocument
import org.openclover.core.reporters.pdf.api.PdfMargins
import org.openclover.core.reporters.pdf.api.PdfPageSize

class PdfBoxDocumentFactoryTest extends TestCase {

    private final PdfBoxDocumentFactory factory = new PdfBoxDocumentFactory()

    /**
     * The description ends up in the report's metadata, and is asked of the library itself so that
     * it cannot drift from the version actually bundled.
     */
    void testTheLibraryDescriptionCarriesARealVersion() {
        String description = factory.getLibraryDescription()

        assertTrue(description, description.startsWith("Apache PDFBox "))
        assertTrue("expected a version number, got: " + description,
                (description =~ /Apache PDFBox \d+\.\d+/).find())
    }

    void testCreatesADocumentWritingToTheGivenStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PdfDocument document = factory.create(out, PdfPageSize.A4,
                new PdfMargins(25d, 25d, 25d, 35d), null)

        assertNotNull(document)
        assertEquals("nothing is written before the document is closed", 0, out.size())

        document.close()
        assertTrue("a closed document is written out", out.size() > 0)
    }
}
