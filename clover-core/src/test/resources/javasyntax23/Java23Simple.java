/**
 * Thin '--source 23' wiring check. Java 23 adds no parser-visible finalized language
 * syntax; this fixture only confirms the source level is accepted and instruments cleanly.
 */
public class Java23Simple {
    public static void main(String[] args) {
        int value = compute(6);
        System.out.println("value = " + value);
    }

    static int compute(int n) {
        return n * 7;
    }
}
