package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfTextRun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Breaks a {@link PdfText} — a sequence of differently-styled runs — into lines that fit a given
 * width. Newlines inside a run break the line explicitly; everything else wraps on whitespace.
 */
class TextLayouter {

    /** Used for the height of a blank line when no run states a size. */
    private static final double DEFAULT_FONT_SIZE = 10;

    /**
     * Slack, in points, when deciding whether a word still fits. A line's width is the sum of its
     * pieces, so it can drift from a single measurement of the same text by a rounding error;
     * without this, text laid out into a box measured to its own width could wrap against itself.
     */
    private static final double FIT_TOLERANCE = 0.01;

    /** A stretch of a single line sharing one font. */
    static class Piece {
        final String text;
        final PdfFontSpec font;
        final String anchor;
        final double width;

        Piece(String text, PdfFontSpec font, String anchor, double width) {
            this.text = text;
            this.font = font;
            this.anchor = anchor;
            this.width = width;
        }
    }

    static class Line {
        final List<Piece> pieces = new ArrayList<>();
        double width;
        double maxFontSize;

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

    List<Line> layout(PdfText text, double maxWidth) {
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
        final double fallbackSize = firstFontSize(text);
        lines.stream()
                .filter(line -> line.maxFontSize == 0)
                .forEach(line -> line.maxFontSize = fallbackSize);
        return lines;
    }

    /**
     * @return the line that is still open after appending {@code content}
     */
    private Line appendWrapped(List<Line> lines, Line current, String content,
                               PdfTextRun run, double maxWidth) {
        if (content.isEmpty()) {
            return current;
        }
        for (String word : splitKeepingTrailingSpaces(content)) {
            double wordWidth = fonts.stringWidth(word, run.getFont());

            if (!current.isEmpty() && current.width + wordWidth > maxWidth + FIT_TOLERANCE) {
                lines.add(current);
                current = new Line();
                // a fresh line never re-wraps leading whitespace
                word = trimLeadingSpaces(word);
                if (word.isEmpty()) {
                    continue;
                }
                wordWidth = fonts.stringWidth(word, run.getFont());
            }

            // A word wider than the whole line is broken mid-word rather than allowed to spill
            // out of its cell - long column headers rely on this.
            while (maxWidth > 0 && current.width + wordWidth > maxWidth + FIT_TOLERANCE) {
                int fitting = charactersFitting(word, run, maxWidth - current.width);
                if (fitting == 0) {
                    if (!current.isEmpty()) {
                        // retry against the full width of an empty line
                        lines.add(current);
                        current = new Line();
                        continue;
                    }
                    // not even one character fits; emit it regardless, to guarantee progress
                    fitting = 1;
                }
                if (fitting >= word.length()) {
                    break;
                }
                final String head = word.substring(0, fitting);
                current.add(new Piece(head, run.getFont(), run.getAnchor(),
                        fonts.stringWidth(head, run.getFont())));
                lines.add(current);
                current = new Line();
                word = word.substring(fitting);
                wordWidth = fonts.stringWidth(word, run.getFont());
            }

            current.add(new Piece(word, run.getFont(), run.getAnchor(), wordWidth));
        }
        return current;
    }

    /**
     * @return how many leading characters of {@code word} fit into {@code available} points
     */
    private int charactersFitting(String word, PdfTextRun run, double available) {
        int count = 0;
        while (count < word.length()
                && fonts.stringWidth(word.substring(0, count + 1), run.getFont()) <= available) {
            count++;
        }
        return count;
    }

    /**
     * Splits on spaces, keeping each space attached to the word before it so that wrapping never
     * loses or duplicates whitespace.
     */
    private static List<String> splitKeepingTrailingSpaces(String content) {
        // split after a space that is not itself followed by one, so a run of spaces stays
        // attached to the word before it
        return Arrays.asList(content.split("(?<= )(?! )"));
    }

    private static String trimLeadingSpaces(String word) {
        return word.replaceAll("^ +", "");
    }

    private static double firstFontSize(PdfText text) {
        return text.getRuns().stream()
                .findFirst()
                .map(run -> run.getFont().getSize())
                .orElse(DEFAULT_FONT_SIZE);
    }

    /**
     * Line spacing for a line, following the {@code fixed + multiplied * fontSize} model the
     * reports were authored against.
     */
    static double leadingOf(Line line, double fixedLeading, double multipliedLeading) {
        return fixedLeading + multipliedLeading * line.maxFontSize;
    }

    double totalHeight(List<Line> lines, double fixedLeading, double multipliedLeading) {
        return lines.stream()
                .mapToDouble(line -> leadingOf(line, fixedLeading, multipliedLeading))
                .sum();
    }
}
