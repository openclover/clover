public class Java14SwitchExpressionWithCaseColon {

    static void yieldInSwitchExpression() {
        // ALLOWED
        int j = 0, k;
        k = switch (j) {
            case 0:
                yield 10;
            default:
                yield 11;
        };

        // ALLOWED
        foo(switch (k) {
            case 10:
                yield 20;
            case 11:
                yield 21;
            default:
                yield 22;
        });
    }

    static void foo(int i) { }

}
