/**
 * This test file fails to instrument and fails to compile.
 */
public class Java14SwitchExpressionMixedCasesFailed {

    static void mixedCaseWithColonWithCaseWithLambda(int i) {
        // NOT ALLOWED - different case kinds used in the same switch
        int j = switch (i) {
            case 0: yield 0;
            case 1 -> 10;
            default:
                yield -1;
        };
    }
}
