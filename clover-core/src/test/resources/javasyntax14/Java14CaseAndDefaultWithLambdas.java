public class Java14CaseAndDefaultWithLambdas {
    enum Colors {
        R, G, B;
    }

    enum EvenOrOdd {
        EVEN, ODD, UNKNOWN;
    }

    static void switchExpressionWithCasesOnly(Colors c) {
        String name = switch (c) {
            case R -> "red";
            case G -> "green";
            case B -> "blue";
        };
    }

    static void switchExpressionWithCasesReturningVoidOnly(Colors c) {
        switch (c) {
            case R -> System.out.println("red");
            case G -> System.out.println("green");
            case B -> System.out.println("blue");
        }
    }

    static void switchExpressionWithCaseAndDefault(int i) {
        EvenOrOdd evenOrOdd = switch (i) {
            case 0 -> EvenOrOdd.EVEN;
            case 1 -> EvenOrOdd.ODD;
            default -> EvenOrOdd.UNKNOWN;
        };
    }

    static void switchExpressionWithCaseAndDefaultReturningVoid(int i) {
        switch (i) {
            case 0 -> System.out.println(EvenOrOdd.EVEN);
            case 1 -> System.out.println(EvenOrOdd.ODD);
            default -> System.out.println(EvenOrOdd.UNKNOWN);
        }
    }

    static void switchExpressionWithExpressionsBlocksAndThrows(int i) {
        // a code after -> can be one of: expression, block, throw
        int result = switch (i) {
            case -1 -> throw new IllegalArgumentException("negative");
            case 0 -> 0;
            default -> {
                int j = i * 10;
                yield j;
            }
        };
    }

    static void switchWithTrowsOnly(int i) {
        // note: it can't return any value, not possible to assign switch to a variable
        switch (i) {
            case -1 -> throw new IllegalArgumentException("negative");
            case 0 -> throw new IllegalArgumentException("zero");
            default -> throw new IllegalArgumentException("anything");
        }
    }

    public static void main(String[] args) {
        switchExpressionWithCasesOnly(Colors.G);
        switchExpressionWithCasesReturningVoidOnly(Colors.B);
        switchExpressionWithCaseAndDefault(2);
        switchExpressionWithCaseAndDefaultReturningVoid(0);
        switchExpressionWithExpressionsBlocksAndThrows(2);
        try {
            switchWithTrowsOnly(2);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }

    }
}
