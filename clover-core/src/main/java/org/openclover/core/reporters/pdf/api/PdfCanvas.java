package org.openclover.core.reporters.pdf.api;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Drawing surface handed to a {@link PdfWidget} so it can paint into a rectangle the layout
 * engine has already resolved.
 */
public interface PdfCanvas {

    void setLineWidth(double width);

    void fillRect(PdfRect rect, Color colour);

    void strokeRect(PdfRect rect, Color colour);

    void drawLine(double x1, double y1, double x2, double y2, Color colour);

    /**
     * Draws an image loaded from the classpath, scaled to fill the given rectangle.
     *
     * @param resourcePath classpath location of the image, e.g. {@code pdf_res/logo1.png}
     */
    void drawImage(String resourcePath, PdfRect bounds);

    /**
     * Draws text inside the given rectangle, vertically centred and single-spaced.
     */
    void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal alignment);

    /**
     * Draws text wrapped to the width of {@code bounds}.
     *
     * @param fixedLeading      line spacing is {@code fixed + multiplied * fontSize}
     * @param multipliedLeading see {@code fixedLeading}
     */
    void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal horizontal,
                  PdfAlign.Vertical vertical, double fixedLeading, double multipliedLeading);

    /**
     * Opens an AWT drawing context mapped onto the given rectangle. The returned scope must be
     * closed once the caller is done painting, which is when the drawing is committed to the page.
     */
    GraphicsScope beginGraphics(PdfRect bounds);

    /**
     * An open AWT drawing context. Closing it commits what was painted onto the page, so it is
     * meant to be used in a try-with-resources block.
     */
    interface GraphicsScope extends AutoCloseable {

        Graphics2D getGraphics();

        @Override
        void close();
    }
}
