package org.openclover.core.reporters.pdf.pdfbox;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfRect;

import java.awt.Graphics2D;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * An open AWT drawing context together with the rectangle it is mapped onto, so the two cannot
 * come apart: closing the scope stamps what was painted onto the page at those coordinates.
 *
 * <p>Closing twice is harmless, which is what lets
 * {@link PdfCanvas#inGraphicsScope(PdfRect, java.util.function.Consumer)} close the scope without
 * having to know whether the caller already did.
 */
class PdfBoxGraphicsScope implements PdfCanvas.GraphicsScope {

    private final PdfBoxGraphics2D graphics;
    private final PDPageContentStream stream;
    private final PdfRect bounds;

    private boolean closed;

    PdfBoxGraphicsScope(PdfBoxGraphics2D graphics, PDPageContentStream stream, PdfRect bounds) {
        this.graphics = graphics;
        this.stream = stream;
        this.bounds = bounds;
    }

    @Override
    public Graphics2D getGraphics() {
        return graphics;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        graphics.dispose();
        final PDFormXObject form = graphics.getXFormObject();
        try {
            stream.saveGraphicsState();
            stream.transform(Matrix.getTranslateInstance((float) bounds.getX(), (float) bounds.getY()));
            stream.drawForm(form);
            stream.restoreGraphicsState();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
