/**
 * Thin '--source 24' wiring check. Java 24 adds no finalized language syntax (all its
 * language JEPs are preview iterations), so this fixture only confirms the source level
 * is accepted and instruments cleanly.
 */
public class Java24Simple {
    public static void main(String[] args) {
        System.out.println("value = " + compute(8));
    }

    static int compute(int n) {
        return n * 3;
    }
}
