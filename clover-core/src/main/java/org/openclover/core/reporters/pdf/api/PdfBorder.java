package org.openclover.core.reporters.pdf.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The edges of a cell that are drawn. Edges are combined as a {@link Set}, so an empty set means
 * a borderless cell and there is no "empty edge" value needing special treatment.
 */
public enum PdfBorder {

    TOP, BOTTOM, LEFT, RIGHT;

    /** No edge at all. */
    public static final Set<PdfBorder> NONE = Collections.unmodifiableSet(EnumSet.noneOf(PdfBorder.class));

    /** Every edge. */
    public static final Set<PdfBorder> BOX = Collections.unmodifiableSet(EnumSet.allOf(PdfBorder.class));

    /**
     * @return an immutable set of the given edges, e.g. {@code PdfBorder.of(TOP, BOTTOM)}
     */
    public static Set<PdfBorder> of(PdfBorder... edges) {
        final EnumSet<PdfBorder> set = EnumSet.noneOf(PdfBorder.class);
        Collections.addAll(set, edges);
        return Collections.unmodifiableSet(set);
    }
}
