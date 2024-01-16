/**
 * This test file fails to instrument and fails to compile.
 */
public class Java14SwitchExpressionWithCaseColonFailed {

    static void breakInSwitchExpression() {
        // NOT ALLOWED - break out of switch expression is not allowed
        // + switch expression does not have any result expressions
        int i = 0, j;
        j = switch (i) {
            case 0:
                break;
            default:
                break;
        };
    }
}
