package org.openclover.core.reporters.pdf.api;

import java.io.IOException;

/**
 * A PDF being built. Tables are appended in order and flow onto further pages as needed; the
 * document is only written out when it is closed.
 */
public interface PdfDocument extends AutoCloseable {

    /**
     * Appends a table, breaking it across pages a whole row at a time.
     */
    void add(PdfTable table) throws IOException;

    /**
     * Starts a new page. Does nothing if the current page is still empty.
     */
    void newPage() throws IOException;

    void setTitle(String title);

    void setCreator(String creator);

    /**
     * Runs the page decorator over every page and writes the document to its destination.
     */
    @Override
    void close() throws IOException;
}
