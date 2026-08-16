package org.openclover.core.reporters.pdf.pdfbox;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextForcedDrawer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.util.Matrix;
import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.runtime.Logger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Drawing primitives on top of a single PDFBox page. Everything the layout engine and the widgets
 * put on a page goes through here.
 */
class PdfBoxCanvas implements PdfCanvas {

    private final PDDocument document;
    private final PDPage page;
    private final PDPageContentStream stream;
    private final FontRegistry fonts;
    private final TextLayouter layouter;
    private final Map<String, PDImageXObject> imageCache;

    PdfBoxCanvas(PDDocument document, PDPage page, PDPageContentStream stream, FontRegistry fonts,
                 Map<String, PDImageXObject> imageCache) {
        this.document = document;
        this.page = page;
        this.stream = stream;
        this.fonts = fonts;
        this.layouter = new TextLayouter(fonts);
        this.imageCache = imageCache;
    }

    /**
     * Narrows a coordinate to the {@code float} PDFBox works in. The layout engine computes in
     * {@code double}; only the final write-out is narrowed, so rounding cannot accumulate.
     */
    private static float f(double value) {
        return (float) value;
    }

    FontRegistry getFonts() {
        return fonts;
    }

    TextLayouter getLayouter() {
        return layouter;
    }

    @Override
    public void setLineWidth(double width) {
        try {
            stream.setLineWidth(f(width));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void fillRect(PdfRect rect, Color colour) {
        if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
            return;
        }
        try {
            stream.setNonStrokingColor(colour);
            stream.addRect(f(rect.getX()), f(rect.getY()), f(rect.getWidth()), f(rect.getHeight()));
            stream.fill();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void strokeRect(PdfRect rect, Color colour) {
        if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
            return;
        }
        try {
            stream.setStrokingColor(colour);
            stream.addRect(f(rect.getX()), f(rect.getY()), f(rect.getWidth()), f(rect.getHeight()));
            stream.stroke();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2, Color colour) {
        try {
            stream.setStrokingColor(colour);
            stream.moveTo(f(x1), f(y1));
            stream.lineTo(f(x2), f(y2));
            stream.stroke();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void drawImage(String resourcePath, PdfRect bounds) {
        try {
            final PDImageXObject image = loadImage(resourcePath);
            if (image != null) {
                stream.drawImage(image, f(bounds.getX()), f(bounds.getY()), f(bounds.getWidth()), f(bounds.getHeight()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal alignment) {
        final List<TextLayouter.Line> lines = layouter.layout(text, bounds.getWidth());
        final double totalHeight = layouter.totalHeight(lines, 0, 1);
        double baseline = bounds.getY() + (bounds.getHeight() + totalHeight) / 2;
        for (TextLayouter.Line line : lines) {
            baseline -= TextLayouter.leadingOf(line, 0, 1);
            drawLine(line, alignedX(line, bounds, alignment), baseline);
        }
    }

    private static double alignedX(TextLayouter.Line line, PdfRect bounds, PdfAlign.Horizontal alignment) {
        switch (alignment) {
            case RIGHT:
                return bounds.getRight() - line.width;
            case CENTER:
                return bounds.getX() + (bounds.getWidth() - line.width) / 2;
            default:
                return bounds.getX();
        }
    }

    /**
     * Draws one laid-out line starting at {@code x}, registering a link annotation for any piece
     * that carries an anchor.
     */
    void drawLine(TextLayouter.Line line, double x, double baselineY) {
        double penX = x;
        for (TextLayouter.Piece piece : line.pieces) {
            drawPiece(piece, penX, baselineY);
            penX += piece.width;
        }
    }

    private void drawPiece(TextLayouter.Piece piece, double x, double baselineY) {
        final PdfFontSpec font = piece.font;
        final String text = fonts.sanitise(piece.text, font);
        if (!text.trim().isEmpty()) {
            try {
                stream.beginText();
                stream.setFont(fonts.getFont(font), f(font.getSize()));
                stream.setNonStrokingColor(font.getColour());
                stream.newLineAtOffset(f(x), f(baselineY));
                stream.showText(text);
                stream.endText();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (piece.anchor != null && !piece.anchor.isEmpty() && piece.width > 0) {
            addLinkAnnotation(piece, x, baselineY);
        }
    }

    private void addLinkAnnotation(TextLayouter.Piece piece, double x, double baselineY) {
        final PdfFontSpec font = piece.font;
        final PDAnnotationLink link = new PDAnnotationLink();
        // an invisible border, so that the link is not boxed the way viewers do by default
        final PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(0);
        link.setBorderStyle(border);

        final PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(f(x));
        rect.setLowerLeftY(f(baselineY + fonts.descent(font)));
        rect.setUpperRightX(f(x + piece.width));
        rect.setUpperRightY(f(baselineY + fonts.ascent(font)));
        link.setRectangle(rect);

        final PDActionURI action = new PDActionURI();
        action.setURI(piece.anchor);
        link.setAction(action);

        try {
            page.getAnnotations().add(link);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Graphics2D beginGraphics(PdfRect bounds) {
        try {
            final PdfBoxGraphics2D graphics =
                    new PdfBoxGraphics2D(document, f(bounds.getWidth()), f(bounds.getHeight()));
            // render glyphs as vector outlines, so chart labels do not depend on any font being
            // resolvable at render time
            graphics.setFontTextDrawer(new PdfBoxGraphics2DFontTextForcedDrawer());
            pendingGraphicsBounds.put(graphics, bounds);
            return graphics;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void endGraphics(Graphics2D graphics) {
        final PdfBoxGraphics2D pdfGraphics = (PdfBoxGraphics2D) graphics;
        final PdfRect bounds = pendingGraphicsBounds.remove(pdfGraphics);
        pdfGraphics.dispose();
        final PDFormXObject form = pdfGraphics.getXFormObject();
        try {
            stream.saveGraphicsState();
            stream.transform(Matrix.getTranslateInstance(f(bounds.getX()), f(bounds.getY())));
            stream.drawForm(form);
            stream.restoreGraphicsState();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final Map<Graphics2D, PdfRect> pendingGraphicsBounds = new HashMap<>();

    private PDImageXObject loadImage(String resourcePath) throws IOException {
        if (imageCache.containsKey(resourcePath)) {
            return imageCache.get(resourcePath);
        }
        PDImageXObject image = null;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                Logger.getInstance().warn("PDF report resource not found on the classpath: " + resourcePath);
            } else {
                final BufferedImage buffered = ImageIO.read(in);
                final ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(buffered, "png", png);
                image = PDImageXObject.createFromByteArray(document, png.toByteArray(), resourcePath);
            }
        }
        imageCache.put(resourcePath, image);
        return image;
    }
}
