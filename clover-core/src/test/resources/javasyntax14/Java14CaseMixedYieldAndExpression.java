public class Java14CaseMixedYieldAndExpression {

    static void mixedYieldAndExpressions(int i) {
        // NOT ALLOWED - different case kinds used in the switch
        //int j = switch (i) {
        //    case 0: yield 0;
        //    case 1 -> 10;
        //    default:
        //        yield -1;
        //};
    }

    public static void main(String[] args) {
        mixedYieldAndExpressions(0);
    }
}
