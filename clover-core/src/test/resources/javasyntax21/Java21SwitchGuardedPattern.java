/**
 * JEP 441 - Pattern Matching for switch (finalized in Java 21).
 * Guarded patterns using the contextual 'when' keyword. The guard is a boolean
 * expression and is expected to be branch-instrumented (adds cyclomatic complexity),
 * while the pattern binding variable itself must not be branch-instrumented.
 */
public class Java21SwitchGuardedPattern {
    public static void main(String[] args) {
        System.out.println("5 -> " + classify(5));
        System.out.println("50 -> " + classify(50));
        System.out.println("hi -> " + classify("hi"));
        System.out.println("null -> " + classify(null));
    }

    static String classify(Object obj) {
        return switch (obj) {
            case Integer i when i > 10 -> "big int " + i;
            case Integer i -> "small int " + i;
            case String s when s.length() > 3 -> "long str " + s;
            case String s -> "short str " + s;
            case null -> "null";
            default -> "other";
        };
    }
}
