/**
 * JEP 440 + JEP 441: a record deconstruction pattern combined with a 'when' guard. The optional
 * binding name that may follow the record pattern's ')' must not swallow the 'when' keyword.
 */
public class Java21RecordPatternGuard {
    record Point(int x, int y) {}

    public static void main(String[] args) {
        System.out.println("(3,4) -> " + classify(new Point(3, 4)));
        System.out.println("(0,0) -> " + classify(new Point(0, 0)));
        System.out.println("other -> " + classify("x"));
    }

    static String classify(Object obj) {
        return switch (obj) {
            case Point(int x, int y) when x + y > 0 -> "positive sum";
            case Point(int x, int y) -> "non-positive sum";
            default -> "not a point";
        };
    }
}
