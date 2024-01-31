public class Java14SwitchExpressionCaseAndDefaultWithThrows {

    static void switchExpressionWithExpressionsBlocksAndThrows(int i) {
        // some branches of a switch expression can throw exceptions
        int result = switch (i) {
            case -1 -> throw new IllegalArgumentException("negative");
            default -> 0;
        };
    }

    static void switchWithTrowsOnly(int i) {
        // note: it can't return any value, so it's not possible to assign switch to a variable
        switch (i) {
            case -1 -> throw new IllegalArgumentException("negative");
            case 0 -> throw new IllegalArgumentException("zero");
            default -> throw new IllegalArgumentException("anything");
        }
    }

    public static void main(String[] args) {
        try {
            switchExpressionWithExpressionsBlocksAndThrows(2);
            switchWithTrowsOnly(2);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
