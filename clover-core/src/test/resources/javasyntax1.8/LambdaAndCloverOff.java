/**
 * Test how CLOVER:OFF keyword is honoured by instrumentation of and inside lambda functions.
 */
public class LambdaAndCloverOff {
    interface Map<T> {
        T map(T x);
    }

    /**
     * Test Clover off around lambda assigned to a field
     */
    ///CLOVER:OFF
    Map<String> aroundFieldExpr = e -> null;
    Map<String> aroundFieldBlock = e -> { return null; };
    ///CLOVER:ON

    /**
     * Test Clover off around whole lambda
     */
    public void testCloverOffAroundLambda() {
        ///CLOVER:OFF
        Map aroundLamExpr = e -> null;
        Map aroundLamBlock = e -> { return null; };
        ///CLOVER:ON
    }

    /**
     * Test Clover off around method with lambda inside
     */
    ///CLOVER:OFF
    public void testCloverOffAroundMethod() {
        Map aroundMethodExpr = e -> null;
        Map aroundMethodBlock = e -> { return null; };
    }
    ///CLOVER:ON

    /**
     * Test Clover off inside lambda
     */
    public void testCloverOffInsideLambda() {
        Map<String> insideLamExpr = e ->
            ///CLOVER:OFF
            e.toUpperCase()  /* special case: instrument it; it's an expression-like lambda, so take setting from the method entry */
            ///CLOVER:ON
        ;
        insideLamExpr.map("abc");

        Map<String> insideLamBlock = e -> {
            ///CLOVER:OFF
            return e.toUpperCase();
            ///CLOVER:ON
        };
        insideLamBlock.map("abc");
    }

    /**
     * Test Clover off intersecting the lambda block.
     */
    public void testCloverOffIntersectingLambda() {
        ///CLOVER:ON
        Map<String> intersectLamExpr = e ->  /* method entry is instrumented */
                ///CLOVER:OFF
                e.toUpperCase()              /* special case: should instrument, because method entry was instrumented */
                ;                            /* but method exit is not */
        ///CLOVER:ON
        intersectLamExpr.map("abc");

        ///CLOVER:OFF
        intersectLamExpr = e ->              /* method entry is not instrumented */
                ///CLOVER:ON
                e.toUpperCase()              /* special case: should not instrument, as method entry was not instrumented */
                ;                            /* but method exit is */
        intersectLamExpr.map("abc");

        ///CLOVER:ON
        Map<String> intersectLamBlock = e -> { /* method entry is instrumented */
            ///CLOVER:OFF
            return e.toUpperCase();            /* do not instrument this statement */
        };                                     /* but method exit is not */
        ///CLOVER:ON
        intersectLamBlock.map("abc");

        ///CLOVER:OFF
        intersectLamBlock = e -> {             /* method entry is not instrumented */
            ///CLOVER:ON
            return e.toUpperCase();            /* instrument this statement */
        };                                     /* but method exit is */
        intersectLamBlock.map("abc");
    }

    public static void main(String[] args) {
        LambdaAndCloverOff lam = new LambdaAndCloverOff();
        lam.testCloverOffAroundLambda();
        lam.testCloverOffAroundMethod();
        lam.aroundFieldBlock.map("abc");
        lam.aroundFieldExpr.map("abc");
        lam.testCloverOffInsideLambda();
        lam.testCloverOffIntersectingLambda();
    }

}
