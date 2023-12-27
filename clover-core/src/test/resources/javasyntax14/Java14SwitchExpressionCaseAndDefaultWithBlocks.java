public class Java14SwitchExpressionCaseAndDefaultWithBlocks {

    static Integer switchExpressionWithBlocksReturningValues(int i) {
        int color = switch (i) {
            case 0 -> { yield 0x00; }
            case 1 -> { yield 0x10; }
            default -> { return null; }
        };
        return color;
    }

    static void switchExpressionWithBlocksReturningVoid(int i) {
        // all void, can't assign any value
        switch (i) {
            case 0 -> { System.out.println("0x00"); }
            case 1 -> { System.out.println("0x10"); }
            default -> { System.out.println("0xFF"); }
        }
    }

    static void switchExpressionWithBlocksThrowingExceptions(int i) {
        // case expression blocks can throw exceptions
        int onlyZero = switch (i) {
            case -1 -> {
                throw new IllegalArgumentException("negative");
            }
            case 0 -> 0;
            default -> {
                throw new IllegalArgumentException("positive");
            };
        };
    }

    public static void main(String[] args) {
        try {
            switchExpressionWithBlocksReturningValues(0);
            switchExpressionWithBlocksReturningValues(1);
            switchExpressionWithBlocksReturningValues(2);
            switchExpressionWithBlocksReturningVoid(0);
            switchExpressionWithBlocksReturningVoid(1);
            switchExpressionWithBlocksReturningVoid(2);
            switchExpressionWithBlocksThrowingExceptions(0);
            switchExpressionWithBlocksThrowingExceptions(1);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
