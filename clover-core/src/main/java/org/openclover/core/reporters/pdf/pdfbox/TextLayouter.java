package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfTextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Breaks a {@link PdfText} — a sequence of differently-styled runs — into lines that fit a given
 * width. Newlines inside a run break the line explicitly; everything else wraps on whitespace.
 *
 * <p>Run text is sanitised against the font once, when the paragraph is split, so neither
 * measuring nor drawing has to repeat that work: {@link Piece#text} is always drawable as-is.
 */
class TextLayouter {

    /** Used for the height of a blank line when no run states a size. */
    private static final double DEFAULT_FONT_SIZE = 10.0;

    /**
     * Slack, in points, when deciding whether a word still fits. A line's width is the sum of its
     * pieces, so it can drift from a single measurement of the same text by a rounding error;
     * without this, text laid out into a box measured to its own width could wrap against itself.
     */
    private static final double FIT_TOLERANCE = 0.01;

    /** Splits after a space not followed by one, keeping runs of spaces with the preceding word. */
    private static final Pattern AFTER_WORD = Pattern.compile("(?<= )(?! )");

    private static final Pattern LEADING_SPACES = Pattern.compile("^ +");

    private static final Pattern NEWLINE = Pattern.compile("\n", Pattern.LITERAL);

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
            final String[] paragraphs = NEWLINE.split(fonts.sanitise(run.getText(), run.getFont()), -1);
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
                .filter(line -> line.maxFontSize == 0.0)
                .forEach(line -> line.maxFontSize = fallbackSize);
        return lines;
    }

    /**
     * Appends one paragraph's worth of sanitised text, wrapping as needed.
     *
     * @return the line that is still open afterwards
     */
    private Line appendWrapped(List<Line> lines, Line current, String content,
                               PdfTextRun run, double maxWidth) {
        if (content.isEmpty()) {
            return current;
        }
        for (String word : AFTER_WORD.split(content)) {
            current = appendWord(lines, current, word, run, maxWidth);
        }
        return current;
    }

    /**
     * Places a single word, starting a new line first if it does not fit on the current one.
     *
     * @return the line that is still open afterwards
     */
    private Line appendWord(List<Line> lines, Line current, String word,
                            PdfTextRun run, double maxWidth) {
        double width = fonts.stringWidth(word, run.getFont());

        if (!current.isEmpty() && !fitsOn(current, width, maxWidth)) {
            lines.add(current);
            current = new Line();
            // a fresh line never re-wraps leading whitespace
            word = LEADING_SPACES.matcher(word).replaceFirst("");
            if (word.isEmpty()) {
                return current;
            }
            width = fonts.stringWidth(word, run.getFont());
        }

        return breakOverlongWord(lines, current, word, width, run, maxWidth);
    }

    /**
     * Emits as many head fragments of {@code word} as it takes for the remainder to fit on a line,
     * then adds that remainder. A word wider than a whole line is broken mid-word rather than
     * allowed to spill out of its cell — long column headers rely on this.
     *
     * @return the line that is still open afterwards, with the remainder already on it
     */
    private Line breakOverlongWord(List<Line> lines, Line current, String word, double width,
                                   PdfTextRun run, double maxWidth) {
        while (maxWidth > 0.0 && !fitsOn(current, width, maxWidth)) {
            int fitting = charactersFitting(word, run.getFont(), maxWidth - current.width);
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
            width = fonts.stringWidth(word, run.getFont());
        }

        current.add(new Piece(word, run.getFont(), run.getAnchor(), width));
        return current;
    }

    private static boolean fitsOn(Line line, double width, double maxWidth) {
        return line.width + width <= maxWidth + FIT_TOLERANCE;
    }

    /**
     * Binary-searches the split point, so an overlong word costs a logarithmic number of
     * measurements rather than one per character.
     *
     * @return how many leading characters of {@code word} fit into {@code available} points
     */
    private int charactersFitting(String word, PdfFontSpec font, double available) {
        int low = 0;
        int high = word.length();
        while (low < high) {
            // bias upwards, so that a fitting midpoint always advances the lower bound
            final int mid = (low + high + 1) >>> 1;
            if (fonts.stringWidth(word.substring(0, mid), font) <= available) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
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

    /**
     * Descent is negative, so subtracting it lifts the baseline off the bottom of the line box.
     *
     * @return the deepest descent of any piece on the line
     */
    double descentOf(Line line) {
        return line.pieces.stream()
                .mapToDouble(piece -> fonts.descent(piece.font))
                .min()
                .orElse(0.0);
    }
}
