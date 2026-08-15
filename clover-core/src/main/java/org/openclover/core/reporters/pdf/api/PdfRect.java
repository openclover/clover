package org.openclover.core.reporters.pdf.api;

/**
 * An immutable rectangle in PDF user space: the origin is the bottom-left corner of the page and
 * the y axis grows upwards.
 */
public class PdfRect {

    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public PdfRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getTop() {
        return y + height;
    }

    public float getRight() {
        return x + width;
    }
}
