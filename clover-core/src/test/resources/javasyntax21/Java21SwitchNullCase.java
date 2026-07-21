/**
 * JEP 441 - Pattern Matching for switch (finalized in Java 21).
 * 'case null' and the combined 'case null, default' labels.
 */
public class Java21SwitchNullCase {
    public static void main(String[] args) {
        System.out.println("null -> " + separateNull(null));
        System.out.println("abc -> " + separateNull("abc"));
        System.out.println("null combined -> " + combinedNullDefault(null));
        System.out.println("xyz combined -> " + combinedNullDefault("xyz"));
    }

    static String separateNull(Object obj) {
        return switch (obj) {
            case null -> "was null";
            case String s -> "str " + s;
            default -> "other";
        };
    }

    static String combinedNullDefault(Object obj) {
        return switch (obj) {
            case Integer i -> "int " + i;
            case null, default -> "null or other";
        };
    }
}
