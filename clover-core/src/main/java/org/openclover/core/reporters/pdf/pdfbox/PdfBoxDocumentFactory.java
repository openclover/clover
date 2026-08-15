package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfDocument;
import org.openclover.core.reporters.pdf.api.PdfDocumentFactory;
import org.openclover.core.reporters.pdf.api.PdfMargins;
import org.openclover.core.reporters.pdf.api.PdfPageDecorator;
import org.openclover.core.reporters.pdf.api.PdfPageSize;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Apache PDFBox backed implementation of {@link PdfDocumentFactory}. This class and the package it
 * lives in are the only places aware of which PDF library is in use.
 */
public class PdfBoxDocumentFactory implements PdfDocumentFactory {

    /** Version of the PDF library, reported in the document metadata. */
    public static final String PDF_LIBRARY_VERSION = "Apache PDFBox 3.0.8";

    @Override
    public PdfDocument create(OutputStream out, PdfPageSize pageSize, PdfMargins margins,
                              PdfPageDecorator decorator) throws IOException {
        return new PdfBoxDocument(out, pageSize, margins, decorator);
    }
}
