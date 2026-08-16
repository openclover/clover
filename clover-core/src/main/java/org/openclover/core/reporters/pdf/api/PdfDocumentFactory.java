package org.openclover.core.reporters.pdf.api;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The single seam between the PDF reporter and whichever PDF library backs it. Nothing outside
 * the implementation package needs to know which one that is.
 */
public interface PdfDocumentFactory {

    /**
     * @param out       stream the finished document is written to; closed together with the document
     * @param pageSize  page geometry
     * @param margins   left, right, top and bottom margins in points
     * @param decorator draws the footer on every page, may be null
     */
    PdfDocument create(OutputStream out, PdfPageSize pageSize, PdfMargins margins,
                       PdfPageDecorator decorator) throws IOException;

    /**
     * @return name and version of the backing PDF library, as reported by the library itself and
     *         recorded in the document metadata, e.g. {@code "Apache PDFBox 3.0.8"}
     */
    String getLibraryDescription();
}
