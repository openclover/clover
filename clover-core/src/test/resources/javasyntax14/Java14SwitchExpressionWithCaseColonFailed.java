/**
 * This test file fails to instrument and fails to compile.
 */
public class Java14SwitchExpressionWithCaseColonFailed {

    static void breakInSwitchExpression() {
        // NOT ALLOWED - break out of switch expression is not allowed
        int i = 0, j;
        j = switch (i) {
            case 0:
                break;
            default:
                break;
        };
    }
}
