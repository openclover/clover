/**
 * Fixture for OC-328 - when a flush policy other than "directed" is used, a constructor gets a
 * try-finally block. At Java 25+ the entry inc() is placed before super()/this(), but the explicit
 * constructor invocation must not land inside that try block ("explicit constructor invocation not
 * allowed here"), also when it is preceded by statements (JEP 513).
 */
public class Java25ConstructorWithFlushing {
    static class LicenceExpiredException extends RuntimeException {
        LicenceExpiredException(String message) {
            super(message);
        }
    }

    static class Base {
        final int x;

        Base(int x) {
            this.x = x;
        }
    }

    static class Sub extends Base {
        final int y;

        // super() first
        Sub(int x, int y) {
            super(x);
            this.y = y;
        }

        // this() delegation
        Sub(int x) {
            this(x, 0);
        }

        // prologue statement before super()
        Sub(String text) {
            int parsed = Integer.parseInt(text);
            super(parsed);
            this.y = parsed * 2;
        }

        // prologue statement before this()
        Sub(long value) {
            int narrowed = (int) value;
            this(narrowed, 1);
        }
    }

    public static void main(String[] args) {
        Sub a = new Sub(1, 2);
        Sub b = new Sub(5);
        Sub c = new Sub("7");
        Sub d = new Sub(9L);
        System.out.println("a = " + a.x + " " + a.y);
        System.out.println("b = " + b.x + " " + b.y);
        System.out.println("c = " + c.x + " " + c.y);
        System.out.println("d = " + d.x + " " + d.y);
        System.out.println("message = " + new LicenceExpiredException("expired").getMessage());
    }
}
