package org.openclover.core.reporters.pdf.api;

/**
 * Cell border edges, combined as a bit mask.
 */
public final class PdfBorder {

    public static final int NONE = 0;
    public static final int TOP = 1;
    public static final int BOTTOM = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int BOX = TOP | BOTTOM | LEFT | RIGHT;

    private PdfBorder() {
    }

    /**
     * Whether every one of the given edges is present, so {@code has(borders, TOP | LEFT)} asks
     * for both a top and a left border.
     *
     * <p>{@link #NONE} is rejected rather than accepted as a degenerate mask: it would make the
     * question vacuous, and a caller writing {@code has(borders, NONE)} to test for the absence of
     * borders would get an answer unrelated to what they asked. Use {@link #isNone} for that.
     *
     * @param edges one or more of {@link #TOP}, {@link #BOTTOM}, {@link #LEFT} and {@link #RIGHT}
     * @throws IllegalArgumentException if {@code edges} is {@link #NONE}
     */
    public static boolean has(int borders, int edges) {
        if (edges == NONE) {
            throw new IllegalArgumentException(
                    "NONE is the empty mask, not an edge; use PdfBorder.isNone(borders) instead");
        }
        return (borders & edges) == edges;
    }

    /**
     * @return true when no edge at all is drawn
     */
    public static boolean isNone(int borders) {
        return (borders & BOX) == NONE;
    }
}
