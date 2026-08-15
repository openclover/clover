package org.openclover.core.reporters.pdf.api;

/**
 * Draws page furniture — in practice the report footer. Replaces iText's page-event mechanism:
 * instead of firing while the page is being written, decorators run in a pass over the finished
 * document, which is why {@link PdfPageContext#getTotalPages()} can be relied on.
 */
public interface PdfPageDecorator {

    void decoratePage(PdfPageContext context);
}
