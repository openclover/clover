public class Java14SwitchExpressionWithMultiValueCase {

    static void switchExpressionWithMultipleCaseValues(int i) {
        int k;
        k = switch (i) {
            case 0, 1, 2*3 -> 10;
            default -> 11;
        };
    }

    static void switchExpressionWithNullAsCase(int i) {
        Integer j = Integer.valueOf(i);
        int k = switch (j) {
            case 0, 1, 2 -> 20;
            // the "null" keyword must be also allowed as case value
            // note: pattern matching can be used since Java 17 preview
            // TODO: enable this test for Java 21
            // case null -> 21;
            default -> 21;
        };
    }

    static void switchExpressionWithNullAndDefaultAsCase(int i) {
        Integer j = Integer.valueOf(i);
        int k = switch (j) {
            case 0, 1, 2 -> 30;
            // a special case when "default" is written as "case default:" instead of "default:"
            // note: pattern matching can be used since Java 17 preview
            // TODO: enable this test for Java 21
            // case null, default -> 31;
            default -> 31;
        };
    }
}
