package org.openclover.core.reporters.pdf.api;

/**
 * An immutable rectangle in PDF user space: the origin is the bottom-left corner of the page and
 * the y-axis grows upwards.
 */
public class PdfRect {

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public PdfRect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getTop() {
        return y + height;
    }

    public double getRight() {
        return x + width;
    }
}
