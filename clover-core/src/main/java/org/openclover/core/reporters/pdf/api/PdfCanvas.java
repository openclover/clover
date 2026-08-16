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
     * Draws text inside the given rectangle, vertically centred.
     */
    void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal alignment);

    /**
     * Opens an AWT drawing context mapped onto the given rectangle. The caller must pass the
     * returned object to {@link #endGraphics} once it is done painting.
     */
    Graphics2D beginGraphics(PdfRect bounds);

    void endGraphics(Graphics2D graphics);
}
