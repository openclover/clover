/**
 * JEP 440 - Record Patterns (finalized in Java 21).
 * Record deconstruction patterns in 'instanceof'. The bound components must not
 * be branch-instrumented (mirrors instanceof type-pattern handling from Java 16).
 */
public class Java21RecordPatternInstanceOf {
    record Point(int x, int y) {}

    public static void main(String[] args) {
        Object p = new Point(3, 4);
        System.out.println("sum = " + sum(p));
        System.out.println("not a point = " + sum("nope"));
    }

    static int sum(Object obj) {
        if (obj instanceof Point(int x, int y)) {
            return x + y;
        }
        return -1;
    }
}
