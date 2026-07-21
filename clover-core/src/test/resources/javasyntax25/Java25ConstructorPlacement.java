/**
 * Fixture for verifying constructor entry-instrumentation placement (JEP 513).
 * A classic constructor with an explicit super()/this() as the first statement - this compiles
 * on any JDK, so it can be instrumented at different source levels to assert where the entry
 * RECORDER.inc() lands:
 *  - source < 25: entry inc() is placed AFTER super()/this()
 *  - source 25+ : entry inc() is placed right after '{', BEFORE super()/this()
 */
public class Java25ConstructorPlacement {
    static class Base {
        Base(int x) {}
    }

    static class Sub extends Base {
        final int y;

        Sub(int x, int y) {
            super(x);
            this.y = y;
        }

        Sub(int x) {
            this(x, 0);
        }
    }

    public static void main(String[] args) {
        Sub a = new Sub(1, 2);
        Sub b = new Sub(5);
        System.out.println("a.y = " + a.y);
        System.out.println("b.y = " + b.y);
    }
}
