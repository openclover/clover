package org.openclover.core.reporters.pdf.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a {@link PdfText} run by run. Mutable and reusable: {@link #build()} may be called any
 * number of times, each call taking an independent snapshot of the runs added so far.
 */
public final class PdfTextBuilder {

    private final List<PdfTextRun> runs = new ArrayList<>();

    PdfTextBuilder() {
    }

    public PdfTextBuilder add(String text, PdfFontSpec font) {
        return add(new PdfTextRun(text, font));
    }

    /** Adds a run that links to {@code anchor}. */
    public PdfTextBuilder addLink(String text, PdfFontSpec font, String anchor) {
        return add(new PdfTextRun(text, font, anchor));
    }

    public PdfTextBuilder add(PdfTextRun run) {
        runs.add(run);
        return this;
    }

    /** Appends every run of an already-built text. */
    public PdfTextBuilder add(PdfText text) {
        runs.addAll(text.getRuns());
        return this;
    }

    public boolean isEmpty() {
        return runs.stream().allMatch(run -> run.getText().isEmpty());
    }

    public PdfText build() {
        return new PdfText(new ArrayList<>(runs));
    }
}
