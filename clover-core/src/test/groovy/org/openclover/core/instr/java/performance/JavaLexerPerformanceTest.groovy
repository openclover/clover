package org.openclover.core.instr.java.performance

import antlr.Token
import org.junit.Test
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.instr.java.JavaLexer
import org.openclover.core.util.UnicodeDecodingReader

import static org.junit.Assert.assertTrue

/**
 * Performance test for {@link JavaLexer}.
 *
 * It lexes a ~10kB Java source file that contains unicode characters (including in identifier
 * names) {@link #RUNS} times, gathers timing metrics with nanosecond precision and prints them
 * to stdout. Assertions use a generous 2x margin over the values observed during the run so that
 * the test does not become flaky on slower/faster hardware.
 */
class JavaLexerPerformanceTest {

    /** Number of times the lexer is run over the source during measurement. */
    private static final int RUNS = 50

    /** Number of warm-up runs (JIT compilation) that are not measured. */
    private static final int WARMUP_RUNS = 10

    /** Safety margin applied to the assertions to tolerate hardware differences. */
    private static final double MARGIN = 2.0

    // Baseline values (nanoseconds) observed on the reference machine. Assertions allow up to
    // MARGIN (2x) over these so the test tolerates slower/faster hardware.
    private static final long BASELINE_AVG_NS = 6_300_000L    // observed ~6.2 ms
    private static final long BASELINE_MAX_NS = 12_200_000L   // observed ~12.1 ms
    private static final long BASELINE_STDDEV_NS = 1_400_000L // observed ~1.4 ms

    @Test
    void testLexerPerformance() {
        final String source = generateSource()
        System.out.println("Source size: " + source.getBytes("UTF-8").length + " bytes")

        // warm-up so that JIT compilation does not skew the measurements
        for (int i = 0; i < WARMUP_RUNS; i++) {
            lex(source)
        }

        final long[] durations = new long[RUNS]
        for (int i = 0; i < RUNS; i++) {
            final long start = System.nanoTime()
            final int tokenCount = lex(source)
            durations[i] = System.nanoTime() - start
            assertTrue("Lexer produced no tokens", tokenCount > 0)
        }

        // gather metrics (nanoseconds)
        long min = Long.MAX_VALUE
        long max = Long.MIN_VALUE
        long total = 0
        for (long d : durations) {
            min = Math.min(min, d)
            max = Math.max(max, d)
            total += d
        }
        final double average = total / (double) RUNS

        double varianceSum = 0
        for (long d : durations) {
            final double diff = d - average
            varianceSum += diff * diff
        }
        final double stdDev = Math.sqrt(varianceSum / RUNS)

        // print results to stdout (values in milliseconds for readability)
        System.out.println("JavaLexer performance over " + RUNS + " runs:")
        System.out.printf("  average : %.3f ms (%d ns)%n", average / 1_000_000.0d, (long) average)
        System.out.printf("  min     : %.3f ms (%d ns)%n", min / 1_000_000.0d, min)
        System.out.printf("  max     : %.3f ms (%d ns)%n", max / 1_000_000.0d, max)
        System.out.printf("  std dev : %.3f ms (%d ns)%n", stdDev / 1_000_000.0d, (long) stdDev)

        // assertions against baselines observed during execution, with a huge (2x) margin to
        // cope with hardware differences
        assertTrue("Min duration greater than max", min <= max)
        assertTrue("Average duration too high: " + (long) average + " ns (baseline " + BASELINE_AVG_NS
                + " ns, allowed " + (long) (BASELINE_AVG_NS * MARGIN) + " ns)",
                average <= BASELINE_AVG_NS * MARGIN)
        assertTrue("Max duration too high: " + max + " ns (baseline " + BASELINE_MAX_NS
                + " ns, allowed " + (long) (BASELINE_MAX_NS * MARGIN) + " ns)",
                max <= BASELINE_MAX_NS * MARGIN)
        assertTrue("Standard deviation too high: " + (long) stdDev + " ns (baseline " + BASELINE_STDDEV_NS
                + " ns, allowed " + (long) (BASELINE_STDDEV_NS * MARGIN) + " ns)",
                stdDev <= BASELINE_STDDEV_NS * MARGIN)
    }

    /**
     * Runs the lexer over the given source and returns the number of tokens produced.
     */
    private static int lex(String source) {
        final JavaInstrumentationConfig config = new JavaInstrumentationConfig()
        config.setSourceLevel(SourceLevel.JAVA_9)
        config.setEncoding("UTF-8")
        final JavaLexer lexer = new JavaLexer(
                new UnicodeDecodingReader(new StringReader(source)), config)

        int count = 0
        for (Token token = lexer.nextToken(); token.getType() != Token.EOF_TYPE; token = lexer.nextToken()) {
            count++
        }
        count
    }

    /**
     * Generates a ~10kB Java source file containing unicode characters, including in
     * identifier names (Greek letters, accented Latin letters, etc.).
     */
    private static String generateSource() {
        final StringBuilder sb = new StringBuilder(12 * 1024)
        sb.append("package org.openclover.core.instr.java.performance.sample;\n\n")
        sb.append("import java.util.List;\n")
        sb.append("import java.util.Map;\n\n")
        sb.append("/**\n * Sample class with unicode: éèê αβγ 你好 ☃.\n */\n")
        sb.append("public class Κλασσ {\n\n")

        int i = 0
        while (sb.length() < 8 * 1024) {
            sb.append("    // method with unicode identifiers: åæø μνξ\n")
            sb.append("    private int μέθοδος").append(i).append("(int αργ, String ñáme) {\n")
            sb.append("        int σύνολο = αργ * ").append(i).append(";\n")
            sb.append("        String мессаге = \"value – \" + ñáme + \" = \" + σύνολο;\n")
            sb.append("        for (int ι = 0; ι < αργ; ι++) {\n")
            sb.append("            σύνολο += ι * 2 - ").append(i).append(";\n")
            sb.append("        }\n")
            sb.append("        return σύνολο; // 结果\n")
            sb.append("    }\n\n")
            i++
        }

        sb.append("}\n")
        sb.toString()
    }
}
