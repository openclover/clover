public class Java14SwitchStatementWithCaseColon {

    static void breakInSwitchStatement() {
        // ALLOWED
        int k = 30;
        switch (k) {
            case 30:k++;break;
            default:break;
        }
    }

    enum Values {
        A, B, C, D, Hello
    }

    // comma-separated case labels combined with the old colon syntax
    static boolean multiCaseWithColon(Values u) {
        boolean a = false;
        switch (u) {
            case A, B, C, D:
                a = true;
                break;
        }
        return a;
    }
}
