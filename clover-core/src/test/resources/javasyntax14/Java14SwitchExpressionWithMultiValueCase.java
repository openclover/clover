public class Java14SwitchExpressionWithMultivalueCase {

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
            case null -> 21;
        };
    }

    static void switchExpressionWithNullAndDefaultAsCase(int i) {
        Integer j = Integer.valueOf(i);
        int k = switch (j) {
            case 0, 1, 2 -> 30;
            // a special case when "default" is written as "case default:" instead of "default:"
            case null, default -> 31;
        };
    }
}
