package org.openclover.core.reporters.pdf

import org.openclover.core.reporters.pdf.api.PdfAlign
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfRect
import org.openclover.core.reporters.pdf.api.PdfText

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

/**
 * A {@link PdfCanvas} that records what it was asked to draw instead of writing to a page, so that
 * layout and widget geometry can be asserted on exactly without producing a PDF.
 */
class RecordingCanvas implements PdfCanvas {

    /** A rectangle that was filled or stroked. */
    static class DrawnRect {
        PdfRect rect
        Color colour
        boolean filled
    }

    static class DrawnLine {
        double x1, y1, x2, y2
        Color colour
    }

    static class DrawnText {
        PdfText text
        PdfRect bounds
        PdfAlign.Horizontal horizontal
        PdfAlign.Vertical vertical
        double fixedLeading
        double multipliedLeading

        /** @return the drawn text with all its runs concatenated */
        String plainText() {
            return text.runs.collect { it.text }.join("")
        }
    }

    static class DrawnImage {
        String resourcePath
        PdfRect bounds
    }

    List<DrawnRect> rects = []
    List<DrawnLine> lines = []
    List<DrawnText> texts = []
    List<DrawnImage> images = []
    List<Double> lineWidths = []

    /** Graphics scopes handed out by {@link #beginGraphics}, in the order they were opened. */
    List<RecordingGraphicsScope> graphicsScopes = []

    @Override
    void setLineWidth(double width) {
        lineWidths.add(width)
    }

    @Override
    void fillRect(PdfRect rect, Color colour) {
        rects.add(new DrawnRect(rect: rect, colour: colour, filled: true))
    }

    @Override
    void strokeRect(PdfRect rect, Color colour) {
        rects.add(new DrawnRect(rect: rect, colour: colour, filled: false))
    }

    @Override
    void drawLine(double x1, double y1, double x2, double y2, Color colour) {
        lines.add(new DrawnLine(x1: x1, y1: y1, x2: x2, y2: y2, colour: colour))
    }

    @Override
    void drawImage(String resourcePath, PdfRect bounds) {
        images.add(new DrawnImage(resourcePath: resourcePath, bounds: bounds))
    }

    @Override
    void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal alignment) {
        drawText(text, bounds, alignment, PdfAlign.Vertical.MIDDLE, 0d, 1d)
    }

    @Override
    void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal horizontal,
                  PdfAlign.Vertical vertical, double fixedLeading, double multipliedLeading) {
        texts.add(new DrawnText(text: text, bounds: bounds, horizontal: horizontal,
                vertical: vertical, fixedLeading: fixedLeading, multipliedLeading: multipliedLeading))
    }

    @Override
    PdfCanvas.GraphicsScope beginGraphics(PdfRect bounds) {
        RecordingGraphicsScope scope = new RecordingGraphicsScope(bounds)
        graphicsScopes.add(scope)
        return scope
    }

    /** Filled rectangles only, in the order they were drawn. */
    List<DrawnRect> filled() {
        return rects.findAll { it.filled }
    }

    /** Stroked rectangles only, in the order they were drawn. */
    List<DrawnRect> stroked() {
        return rects.findAll { !it.filled }
    }

    /**
     * Hands out an AWT context backed by an in-memory image, so that a widget painting through
     * {@link PdfCanvas#beginGraphics} can be exercised without any PDF machinery.
     */
    static class RecordingGraphicsScope implements PdfCanvas.GraphicsScope {

        final PdfRect bounds
        boolean closed

        private final BufferedImage image
        private final Graphics2D graphics

        RecordingGraphicsScope(PdfRect bounds) {
            this.bounds = bounds
            this.image = new BufferedImage(Math.max(1, (int) bounds.width),
                    Math.max(1, (int) bounds.height), BufferedImage.TYPE_INT_ARGB)
            this.graphics = image.createGraphics()
        }

        @Override
        Graphics2D getGraphics() {
            return graphics
        }

        @Override
        void close() {
            closed = true
            graphics.dispose()
        }
    }
}
