package org.openclover.core.reporters.pdf.api;

/**
 * Font families known to the PDF reporter. Only a sans-serif family is used today; the enum
 * exists so that further families (e.g. a monospaced one for source listings) can be added
 * without changing the API.
 */
public enum PdfFontFamily {
    SANS
}
