/**
 * Unnamed Variables & Patterns (finalized in Java 22).
 * A record component in a deconstruction pattern may be a bare unnamed pattern '_'.
 * The '_' component introduces no binding and must not be branch-instrumented.
 */
public class Java22UnnamedPatterns {
    record Point(int x, int y) {}

    public static void main(String[] args) {
        Object p = new Point(3, 4);
        System.out.println("x only = " + xOnly(p));
        System.out.println("y only = " + yOnly(p));
        System.out.println("any point = " + anyPoint(p));
        System.out.println("not a point = " + xOnly("nope"));
    }

    static int xOnly(Object obj) {
        if (obj instanceof Point(int x, _)) {
            return x;
        }
        return -1;
    }

    static int yOnly(Object obj) {
        return switch (obj) {
            case Point(_, int y) -> y;
            default -> -1;
        };
    }

    static String anyPoint(Object obj) {
        return switch (obj) {
            case Point(_, _) -> "yes";
            default -> "no";
        };
    }
}
