/**
 * JEP 441 - Pattern Matching for switch (finalized in Java 21).
 * Type patterns in switch case labels, arrow and colon forms.
 */
public class Java21SwitchTypePatterns {
    public static void main(String[] args) {
        System.out.println("arrow Integer = " + describeArrow(42));
        System.out.println("arrow String = " + describeArrow("hello"));
        System.out.println("arrow Object = " + describeArrow(3.14));
        System.out.println("colon Integer = " + describeColon(7));
        System.out.println("colon String = " + describeColon("world"));
        System.out.println("colon Object = " + describeColon(2.71));
    }

    static String describeArrow(Object obj) {
        return switch (obj) {
            case Integer i -> "int " + i;
            case String s -> "str " + s;
            default -> "other " + obj;
        };
    }

    static String describeColon(Object obj) {
        switch (obj) {
            case Integer i:
                return "int " + i;
            case String s:
                return "str " + s;
            default:
                return "other " + obj;
        }
    }
}
