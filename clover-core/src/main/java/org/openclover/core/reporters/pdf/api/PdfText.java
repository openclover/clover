package org.openclover.core.reporters.pdf.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A paragraph made of one or more {@link PdfTextRun}s. Newline characters inside a run force a
 * line break; otherwise runs flow and wrap to the width they are laid out in.
 */
public class PdfText implements PdfCellContent {

    private final List<PdfTextRun> runs = new ArrayList<>();

    public static PdfText of(String text, PdfFontSpec font) {
        return new PdfText().add(text, font);
    }

    public PdfText add(String text, PdfFontSpec font) {
        return add(new PdfTextRun(text, font));
    }

    public PdfText addLink(String text, PdfFontSpec font, String anchor) {
        return add(new PdfTextRun(text, font, anchor));
    }

    public PdfText add(PdfTextRun run) {
        runs.add(run);
        return this;
    }

    public PdfText add(PdfText other) {
        runs.addAll(other.runs);
        return this;
    }

    public List<PdfTextRun> getRuns() {
        return Collections.unmodifiableList(runs);
    }

    public boolean isEmpty() {
        return runs.stream().allMatch(run -> run.getText().isEmpty());
    }
}
