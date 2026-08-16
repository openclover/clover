package org.openclover.core.reporters.pdf.api;

import java.util.Collections;
import java.util.List;

/**
 * A paragraph made of one or more {@link PdfTextRun}s. Newline characters inside a run force a
 * line break; otherwise runs flow and wrap to the width they are laid out in.
 *
 * <p>Immutable: a text placed in a cell keeps the content it had when it was added. Assemble one
 * with a {@link PdfTextBuilder}, or with {@link #of} for the common single-run case.
 */
public final class PdfText implements PdfCellContent {

    private final List<PdfTextRun> runs;

    PdfText(List<PdfTextRun> runs) {
        this.runs = Collections.unmodifiableList(runs);
    }

    public static PdfTextBuilder builder() {
        return new PdfTextBuilder();
    }

    /** A text of a single run. */
    public static PdfText of(String text, PdfFontSpec font) {
        return builder().add(text, font).build();
    }

    /** A text of a single run acting as an external link. */
    public static PdfText ofLink(String text, PdfFontSpec font, String anchor) {
        return builder().addLink(text, font, anchor).build();
    }

    public List<PdfTextRun> getRuns() {
        return runs;
    }

    /** @return a builder holding this text's runs, for deriving a longer text from it */
    public PdfTextBuilder toBuilder() {
        return new PdfTextBuilder().add(this);
    }

    public boolean isEmpty() {
        return runs.stream().allMatch(run -> run.getText().isEmpty());
    }

    @Override
    public PdfMeasuredContent measure(PdfLayout layout, PdfCellStyle style, double contentWidth) {
        return layout.measureText(this, contentWidth, style.getHorizontalAlignment(),
                style.getFixedLeading(), style.getMultipliedLeading());
    }
}
