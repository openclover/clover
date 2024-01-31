/**
 * This test file fails to instrument and fails to compile.
 */
public class Java14SwitchStatementWithCaseColonFailed {

    static void yieldInSwitchStatementIsNotAllowed() {
        // NOT ALLOWED - yield outside of switch expression
        int i = 0;
        switch (i) {
            case 0:
                yield 0;
            default:
                yield 1;
        };
    }
}
