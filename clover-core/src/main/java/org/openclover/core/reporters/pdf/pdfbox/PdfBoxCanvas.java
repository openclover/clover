package org.openclover.core.reporters.pdf.pdfbox;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextForcedDrawer;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfText;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

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
    private final ImageRegistry images;

    PdfBoxCanvas(PDDocument document, PDPage page, PDPageContentStream stream, FontRegistry fonts,
                 TextLayouter layouter, ImageRegistry images) {
        this.document = document;
        this.page = page;
        this.stream = stream;
        this.fonts = fonts;
        this.layouter = layouter;
        this.images = images;
    }

    /**
     * Narrows a coordinate to the {@code float} PDFBox works in. The layout engine computes in
     * {@code double}; only the final write-out is narrowed, so rounding cannot accumulate.
     */
    private static float f(double value) {
        return (float) value;
    }

    /** A drawing operation on the content stream, which PDFBox declares as throwing. */
    @FunctionalInterface
    private interface StreamOp {
        void run() throws IOException;
    }

    /**
     * Runs a drawing operation, turning PDFBox's checked {@link IOException} into an unchecked one:
     * a content stream that cannot be written to is not something a widget can recover from.
     */
    private static void draw(StreamOp op) {
        try {
            op.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void setLineWidth(double width) {
        draw(() -> stream.setLineWidth(f(width)));
    }

    @Override
    public void fillRect(PdfRect rect, Color colour) {
        if (rect.getWidth() <= 0.0 || rect.getHeight() <= 0.0) {
            return;
        }
        draw(() -> {
            stream.setNonStrokingColor(colour);
            stream.addRect(f(rect.getX()), f(rect.getY()), f(rect.getWidth()), f(rect.getHeight()));
            stream.fill();
        });
    }

    @Override
    public void strokeRect(PdfRect rect, Color colour) {
        if (rect.getWidth() <= 0.0 || rect.getHeight() <= 0.0) {
            return;
        }
        draw(() -> {
            stream.setStrokingColor(colour);
            stream.addRect(f(rect.getX()), f(rect.getY()), f(rect.getWidth()), f(rect.getHeight()));
            stream.stroke();
        });
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2, Color colour) {
        draw(() -> {
            stream.setStrokingColor(colour);
            stream.moveTo(f(x1), f(y1));
            stream.lineTo(f(x2), f(y2));
            stream.stroke();
        });
    }

    @Override
    public void drawImage(String resourcePath, PdfRect bounds) {
        final PDImageXObject image = images.get(resourcePath);
        draw(() -> stream.drawImage(image,
                f(bounds.getX()), f(bounds.getY()), f(bounds.getWidth()), f(bounds.getHeight())));
    }

    @Override
    public void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal alignment) {
        drawText(text, bounds, alignment, PdfAlign.Vertical.MIDDLE, 0.0, 1.0);
    }

    @Override
    public void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal horizontal,
                         PdfAlign.Vertical vertical, double fixedLeading, double multipliedLeading) {
        final List<TextLayouter.Line> lines = layouter.layout(text, bounds.getWidth());
        final double totalHeight = layouter.totalHeight(lines, fixedLeading, multipliedLeading);

        double lineTop = topOf(bounds, totalHeight, vertical);
        for (TextLayouter.Line line : lines) {
            lineTop -= TextLayouter.leadingOf(line, fixedLeading, multipliedLeading);
            // the descent is negative, so subtracting it lifts the baseline off the line box's
            // bottom edge and keeps descenders inside the box
            drawTextLine(line, alignedX(line, bounds, horizontal), lineTop - layouter.descentOf(line));
        }
    }

    /**
     * @return the top edge of the first line box, once the whole text block is aligned in its box
     */
    private static double topOf(PdfRect bounds, double totalHeight, PdfAlign.Vertical vertical) {
        switch (vertical) {
            case MIDDLE:
                return bounds.getY() + (bounds.getHeight() + totalHeight) / 2.0;
            case BOTTOM:
                return bounds.getY() + totalHeight;
            default:
                return bounds.getTop();
        }
    }

    private static double alignedX(TextLayouter.Line line, PdfRect bounds, PdfAlign.Horizontal alignment) {
        switch (alignment) {
            case RIGHT:
                return bounds.getRight() - line.width;
            case CENTER:
                return bounds.getX() + (bounds.getWidth() - line.width) / 2.0;
            default:
                return bounds.getX();
        }
    }

    /**
     * Draws one laid-out line starting at {@code x}, registering a link annotation for any piece
     * that carries an anchor.
     */
    private void drawTextLine(TextLayouter.Line line, double x, double baselineY) {
        double penX = x;
        for (TextLayouter.Piece piece : line.pieces) {
            drawPiece(piece, penX, baselineY);
            penX += piece.width;
        }
    }

    private void drawPiece(TextLayouter.Piece piece, double x, double baselineY) {
        final PdfFontSpec font = piece.font;
        // blank text produces no marks, so the whole text object is skipped
        if (StringUtils.isNotBlank(piece.text)) {
            draw(() -> {
                stream.beginText();
                stream.setFont(fonts.getFont(font), f(font.getSize()));
                stream.setNonStrokingColor(font.getColour());
                stream.newLineAtOffset(f(x), f(baselineY));
                stream.showText(piece.text);
                stream.endText();
            });
        }
        if (StringUtils.isNotEmpty(piece.anchor) && piece.width > 0.0) {
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
    public GraphicsScope beginGraphics(PdfRect bounds) {
        try {
            final PdfBoxGraphics2D graphics =
                    new PdfBoxGraphics2D(document, f(bounds.getWidth()), f(bounds.getHeight()));
            // render glyphs as vector outlines, so chart labels do not depend on any font being
            // resolvable at render time
            graphics.setFontTextDrawer(new PdfBoxGraphics2DFontTextForcedDrawer());
            return new PdfBoxGraphicsScope(graphics, stream, bounds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
