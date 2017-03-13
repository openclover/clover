/**
 * Test for instrumentation of branches in case when a generic type is used in a condition,
 * which is related with an implicit conversion to Boolean via autoboxing by javac.
 */
public class BranchCoverageWithAutoboxing {
    interface Data {
        public <T> T getValue();
    }

    // works only under JDK7 and higher
// TODO Uncomment when CLOV-1389 is implemented
//    public boolean testGetValue(Data source) {
//        if (source.getValue()) {  // Implicit conversion to Boolean via autoboxing
//            return true;
//        }
//        return false;
//    }

    public boolean testBooleanExpression(Data source, int j) {
        if (2 + 3 < j) {
            return true;
        }
        return false;
    }

    public boolean testEvenMoreBooleanExpression(Data source, int j) {
        if (2 + 3 < j && (j > 0 || j < - 100)) {
            return true;
        }
        return false;
    }

//    Compilation of this code fails under javac:
//    Error: Operator && cannot be applied to java.lang.Object, boolean
//
//    public boolean testGetValueWithBoolean(Data source) {
//        if (source.getValue() && true) {
//            return true;
//        }
//        return false;
//    }

    /**
     * Example how a call to a generic method can be wrapped by Clover:
     *  - declare Boolean variable before condition block
     *  - assign content of original expression to the variable
     *  - implicit conversion from a generic type to Boolean (like Boolean.valueOf(T.toString)) will occur
     *  - use this variable with a rest of Clover branch instrumentation stuff
     *  - implicit conversion from Boolean to boolean will occur at && operator
     *
     * @param source
     * @return
     */
    public boolean testGenericValueWrapping(Data source) {
        // original: if (source.getValue)
        Boolean b;
        if ( (b = (source.getValue())) && true) { // Explicit conversion
            return true;
        }
        return false;
    }

    /**
     * Example how a call to a boolean expression can be wrapped by Clover:
     *  - declare Boolean variable before condition block
     *  - assign content of original expression to the variable
     *  - implicit conversion from a boolean to Boolean (like Boolean.valueOf(bool)) will occur
     *  - use this variable with a rest of Clover branch instrumentation stuff
     *  - implicit conversion from Boolean to boolean will occur at && operator
     *
     * @param source
     * @return
     */
    public boolean testBooleanExpressionWrapping(Data source) {
        // original: if (2 + 3 < 5)
        Boolean b;
        if ( (b = (2 + 3 < 5)) && true) { // Explicit conversion
            return true;
        }
        return false;
    }

}
