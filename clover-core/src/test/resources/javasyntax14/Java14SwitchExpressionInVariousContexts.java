public class Java14SwitchExpressionInVariousContexts {

    static void switchExpressionInAssignment(int j) {
        int k = switch (j) {
            case 0 -> 10;
            default -> 11;
        };
    }

    static void switchExpressionInExpression(int j) {
        int k = 8 + (switch (j) {
            case 0 -> 20;
            default -> 21;
        } * 24) / 2;

        if (switch (j) {
            case 0 -> 30;
            default -> 31;
        } % 10 == 0) {
            int l = 32;
        }
    }

    static void switchExpressionAsMethodArgument(int k) {
        foo(switch (k) {
            case 10 -> 100;
            default -> 200;
        });
    }

    static void switchExpressionAsMethodStatement(int kk) {
        switch (kk) {
            case 77 -> System.out.println("77");
            default -> System.out.println("not 77");
        }
    }

    // switch expression in an initializer block
    int kkk = 88;
    {
        switch (kkk) {
            case 88 -> System.out.println("88");
            default -> System.out.println("not 88");
        }
    }

    static void switchExpressionInThrowWithThrow(int j) {
        throw switch (j) {
            case 0 -> new RuntimeException("zero");
            case 1 -> new IllegalArgumentException("one");
            default -> throw new IllegalArgumentException("unsupported");
        };
    }
    static void foo(int i) { }

}
