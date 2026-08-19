package org.openclover.core.reporters.pdf.pdfbox;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfDocument;
import org.openclover.core.reporters.pdf.api.PdfMargins;
import org.openclover.core.reporters.pdf.api.PdfPageContext;
import org.openclover.core.reporters.pdf.api.PdfPageDecorator;
import org.openclover.core.reporters.pdf.api.PdfPageSize;
import org.openclover.core.reporters.pdf.api.PdfTable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Content flow on top of PDFBox: tables are appended down the page, rows spill onto the next page
 * when they no longer fit, and the page decorator runs over the finished document just before it
 * is written out — which is why the footer can print a page total without the template patching
 * the old iText-based implementation needed.
 */
class PdfBoxDocument implements PdfDocument {

    private final PDDocument document = new PDDocument();
    private final OutputStream out;
    private final PdfPageSize pageSize;
    private final PdfMargins margins;
    private final PdfPageDecorator decorator;
    private final FontRegistry fonts;
    private final TextLayouter layouter;
    private final TableRenderer tables;
    private final ImageRegistry images;
    private final List<PDPage> pages = new ArrayList<>();

    private PDPage currentPage;
    private PDPageContentStream currentStream;
    private PdfCanvas currentCanvas;
    private double cursorY;
    private boolean pageIsEmpty = true;
    private boolean closed;

    PdfBoxDocument(OutputStream out, PdfPageSize pageSize, PdfMargins margins,
                   PdfPageDecorator decorator) {
        this.out = out;
        this.pageSize = pageSize;
        this.margins = margins;
        this.decorator = decorator;
        this.fonts = new FontRegistry(document);
        this.images = new ImageRegistry(document);
        // one layouter per document: it is stateless apart from the fonts it measures against
        this.layouter = new TextLayouter(fonts);
        this.tables = new TableRenderer(layouter);
    }

    @Override
    public void setTitle(String title) {
        document.getDocumentInformation().setTitle(title);
    }

    @Override
    public void setCreator(String creator) {
        document.getDocumentInformation().setCreator(creator);
        document.getDocumentInformation().setProducer(creator);
    }

    private double contentWidth() {
        return pageSize.getWidth() - margins.getLeft() - margins.getRight();
    }

    private double contentTop() {
        return pageSize.getHeight() - margins.getTop();
    }

    private double contentBottom() {
        return margins.getBottom();
    }

    @Override
    public void add(PdfTable table) throws IOException {
        final TableLayout layout = tables.layout(table, contentWidth(), false);

        for (TableLayout.Row row : layout.getRows()) {
            ensurePage();
            // a row taller than a whole page cannot be split, so it is drawn on a fresh page and
            // allowed to run over rather than being dropped
            if (cursorY - row.getHeight() < contentBottom() && !pageIsEmpty) {
                newPage();
                ensurePage();
            }
            tables.drawRow(currentCanvas, row, margins.getLeft(), cursorY);
            cursorY -= row.getHeight();
            pageIsEmpty = false;
        }
    }

    @Override
    public void newPage() throws IOException {
        if (currentPage == null || pageIsEmpty) {
            return;
        }
        endPage();
    }

    /**
     * Pages are created lazily, so that a trailing {@link #newPage()} — which the report flow ends
     * with — does not leave a blank page behind.
     */
    private void ensurePage() throws IOException {
        if (currentPage != null) {
            return;
        }
        currentPage = new PDPage(new PDRectangle((float) pageSize.getWidth(), (float) pageSize.getHeight()));
        document.addPage(currentPage);
        pages.add(currentPage);
        currentStream = new PDPageContentStream(document, currentPage);
        currentCanvas = new PdfBoxCanvas(document, currentPage, currentStream, fonts, layouter, images);
        cursorY = contentTop();
        pageIsEmpty = true;
    }

    private void endPage() throws IOException {
        currentStream.close();
        currentStream = null;
        currentCanvas = null;
        currentPage = null;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try (PDDocument closeMe = document; OutputStream target = out) {
            if (currentStream != null) {
                endPage();
            }
            if (pages.isEmpty()) {
                // a PDF must have at least one page
                ensurePage();
                endPage();
            }
            decoratePages();
            closeMe.save(target);
        }
    }

    private void decoratePages() throws IOException {
        if (decorator == null) {
            return;
        }
        for (int i = 0; i < pages.size(); i++) {
            final PDPage page = pages.get(i);
            try (PDPageContentStream stream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                final PdfCanvas canvas =
                        new PdfBoxCanvas(document, page, stream, fonts, layouter, images);
                decorator.decoratePage(new PageContext(canvas, i + 1, pages.size()));
            }
        }
    }

    /**
     * What the decorator sees for one page.
     */
    private class PageContext implements PdfPageContext {

        private final PdfCanvas canvas;
        private final int pageNumber;
        private final int totalPages;

        PageContext(PdfCanvas canvas, int pageNumber, int totalPages) {
            this.canvas = canvas;
            this.pageNumber = pageNumber;
            this.totalPages = totalPages;
        }

        @Override
        public int getPageNumber() {
            return pageNumber;
        }

        @Override
        public int getTotalPages() {
            return totalPages;
        }

        @Override
        public double getPageWidth() {
            return pageSize.getWidth();
        }

        @Override
        public double getPageHeight() {
            return pageSize.getHeight();
        }

        @Override
        public PdfMargins getMargins() {
            return margins;
        }

        @Override
        public PdfCanvas getCanvas() {
            return canvas;
        }

        @Override
        public void drawTable(PdfTable table, double x, double topY) {
            tables.drawTable(canvas, table, x, topY, contentWidth());
        }
    }
}
