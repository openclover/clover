public class Java14SwitchStatementWithCaseColon {

    static void breakInSwitchStatement() {
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
}
