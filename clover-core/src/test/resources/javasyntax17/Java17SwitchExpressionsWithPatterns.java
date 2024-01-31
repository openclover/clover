public class Java17SwitchExpressionsWithPatterns {
    enum EvenOrOdd {
        EVEN, ODD, UNKNOWN;
    }

    static void switchExpressionWithNullAndDefault(Integer i) {
        // in a traditional switch it's possible to define case ...:, followed by default: with no break
        // to allow the same behaviour in expressions, the default keyword can be used also inside case pattern
        EvenOrOdd evenOrOdd = switch(i) {
            case 1 -> EvenOrOdd.ODD;
            case 2 -> EvenOrOdd.EVEN;
            // note: pattern matching is available in Java 17-preview
            case null, default -> EvenOrOdd.UNKNOWN;
        };
    }

    public static void main(String[] args) {
        switchExpressionWithNullAndDefault(1);
        switchExpressionWithNullAndDefault(2);
        switchExpressionWithNullAndDefault(null);
    }
}
