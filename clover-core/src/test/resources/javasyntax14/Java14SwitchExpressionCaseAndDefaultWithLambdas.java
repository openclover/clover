public class Java14SwitchExpressionCaseAndDefaultWithLambdas {
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

    static void switchExpressionWithCaseReferencingNonFinalVariable() {
        for (int i = 0; i < 3; i++) {
            switch (i) {
                case 0 -> System.out.println("zero:" + i);
                case 1 -> System.out.println("one:" + i);
                default -> System.out.println("other:" + i);
            }
        }
    }

    static void switchExpressionWithIgnoredValuesHaveNoYield(int i) {
        // a non-standard case when values returned by switch case blocks are ignored
        // code emitter shall not try to add yield, even if a method called returns a value
        // otherwise compiler fails with "yield outside of switch expression"
        switch (i) {
            case 1 -> foo(1);
            case 2 -> foo(2);
            default -> foo(3);
        }
    }

    static int foo(int i) {
        return i;
    }

    public static void main(String[] args) {
        switchExpressionWithCasesOnly(Colors.G);
        switchExpressionWithCasesReturningVoidOnly(Colors.B);
        switchExpressionWithCaseAndDefault(2);
        switchExpressionWithCaseAndDefaultReturningVoid(0);
        switchExpressionWithCaseReferencingNonFinalVariable();
        switchExpressionWithIgnoredValuesHaveNoYield(1);
    }
}
