/**
 * JEP 440 - Record Patterns (finalized in Java 21).
 * Nested record deconstruction patterns in switch, including 'var' components.
 */
public class Java21NestedRecordPattern {
    record Point(int x, int y) {}
    record Line(Point from, Point to) {}

    public static void main(String[] args) {
        Object line = new Line(new Point(0, 0), new Point(3, 4));
        System.out.println("describe = " + describe(line));
        System.out.println("describe point = " + describe(new Point(1, 2)));
        System.out.println("describe other = " + describe("x"));
    }

    static String describe(Object obj) {
        return switch (obj) {
            case Line(Point(var x1, var y1), Point(var x2, var y2)) ->
                    "line (" + x1 + "," + y1 + ")->(" + x2 + "," + y2 + ")";
            case Point(var x, var y) -> "point (" + x + "," + y + ")";
            default -> "other";
        };
    }
}
