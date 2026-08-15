package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfTextRun;

import java.util.ArrayList;
import java.util.List;

/**
 * Breaks a {@link PdfText} — a sequence of differently-styled runs — into lines that fit a given
 * width. Newlines inside a run break the line explicitly; everything else wraps on whitespace.
 */
class TextLayouter {

    /** A stretch of a single line sharing one font. */
    static class Piece {
        final String text;
        final PdfFontSpec font;
        final String anchor;
        final float width;

        Piece(String text, PdfFontSpec font, String anchor, float width) {
            this.text = text;
            this.font = font;
            this.anchor = anchor;
            this.width = width;
        }
    }

    static class Line {
        final List<Piece> pieces = new ArrayList<>();
        float width;
        float maxFontSize;

        void add(Piece piece) {
            pieces.add(piece);
            width += piece.width;
            maxFontSize = Math.max(maxFontSize, piece.font.getSize());
        }

        boolean isEmpty() {
            return pieces.isEmpty();
        }
    }

    private final FontRegistry fonts;

    TextLayouter(FontRegistry fonts) {
        this.fonts = fonts;
    }

    List<Line> layout(PdfText text, float maxWidth) {
        final List<Line> lines = new ArrayList<>();
        Line current = new Line();

        for (PdfTextRun run : text.getRuns()) {
            final String[] paragraphs = run.getText().split("\n", -1);
            for (int p = 0; p < paragraphs.length; p++) {
                if (p > 0) {
                    lines.add(current);
                    current = new Line();
                }
                current = appendWrapped(lines, current, paragraphs[p], run, maxWidth);
            }
        }
        lines.add(current);

        // a line carrying no pieces at all still occupies a line box; give it the run's font size
        // so that blank lines keep their height
        for (Line line : lines) {
            if (line.maxFontSize == 0f) {
                line.maxFontSize = firstFontSize(text);
            }
        }
        return lines;
    }

    /**
     * @return the line that is still open after appending {@code content}
     */
    private Line appendWrapped(List<Line> lines, Line current, String content,
                               PdfTextRun run, float maxWidth) {
        if (content.isEmpty()) {
            return current;
        }
        for (String word : splitKeepingTrailingSpaces(content)) {
            final float wordWidth = fonts.stringWidth(word, run.getFont());
            final boolean fits = current.isEmpty() || current.width + wordWidth <= maxWidth;
            if (!fits) {
                lines.add(current);
                current = new Line();
                // a fresh line never re-wraps leading whitespace
                word = trimLeadingSpaces(word);
                if (word.isEmpty()) {
                    continue;
                }
            }
            current.add(new Piece(word, run.getFont(), run.getAnchor(),
                    fonts.stringWidth(word, run.getFont())));
        }
        return current;
    }

    /**
     * Splits on spaces, keeping each space attached to the word before it so that wrapping never
     * loses or duplicates whitespace.
     */
    private static List<String> splitKeepingTrailingSpaces(String content) {
        final List<String> words = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == ' ') {
                // consume the whole run of spaces into the current word
                int end = i + 1;
                while (end < content.length() && content.charAt(end) == ' ') {
                    end++;
                }
                words.add(content.substring(start, end));
                start = end;
                i = end - 1;
            }
        }
        if (start < content.length()) {
            words.add(content.substring(start));
        }
        return words;
    }

    private static String trimLeadingSpaces(String word) {
        int i = 0;
        while (i < word.length() && word.charAt(i) == ' ') {
            i++;
        }
        return word.substring(i);
    }

    private static float firstFontSize(PdfText text) {
        for (PdfTextRun run : text.getRuns()) {
            return run.getFont().getSize();
        }
        return 10f;
    }

    /**
     * Line spacing for a line, following the {@code fixed + multiplied * fontSize} model the
     * reports were authored against.
     */
    static float leadingOf(Line line, float fixedLeading, float multipliedLeading) {
        return fixedLeading + multipliedLeading * line.maxFontSize;
    }

    float totalHeight(List<Line> lines, float fixedLeading, float multipliedLeading) {
        float height = 0f;
        for (Line line : lines) {
            height += leadingOf(line, fixedLeading, multipliedLeading);
        }
        return height;
    }
}
