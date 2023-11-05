public class Java14CaseColonBreakYield {

    static void yieldInSwitchExpression() {
        // NOT ALLOWED - yield outside of switch expression
        //int i = 0;
        //switch (i) {
        //    case 0:
        //        yield 0;
        //    default:
        //        yield 1;
        //};

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

    static void breakInSwitchStatement() {
        // NOT ALLOWED - break out of switch expression is not allowed
        //int i = 0, j;
        //j = switch (i) {
        //    case 0:
        //        break;
        //    default:
        //        break;
        //};

        // ALLOWED
        int k = 30;
        switch (k) {
            case 30:
                k++;
                break;
            default:
                break;
        }
    }

    static void foo(int i) { }

    public static void main(String[] args) {
        yieldInSwitchExpression();
        breakInSwitchStatement();
    }
}
