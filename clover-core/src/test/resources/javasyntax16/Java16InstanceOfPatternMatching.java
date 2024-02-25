import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Java16InstanceOfPatternMatching {
    static class A {
        static class B {

        }
    }

    public static void main(String[] args) {
        instanceOfCasting();
        instanceOfCastingWithFinal();
        instanceOfCastingWithArray();
        instanceOfCastingWithGenerics();
        instanceOfCastingWithGenericsWildcard();
        instanceOfInExpressions();
        instanceOfCastingVisibilityLimitations();
        detectionOfInstanceOfWithCasting();
    }

    private static void instanceOfCasting() {
        Object obj = "a string";
        if (obj instanceof String str) {
            System.out.println("obj is String = " + str);
        } else {
            System.out.println("obj is Object = " + obj);
        }
    }

    private static void instanceOfCastingWithFinal() {
        Object obj = "a final string";
        if (obj instanceof final String str) {
            System.out.println("obj is final String = " + str);
        } else {
            System.out.println("obj is final Object = " + obj);
        }
    }

    private static void instanceOfCastingWithArray() {
        Object obj = new String[] { "a string array" };
        if (obj instanceof String[] str) {
            System.out.println("obj is String[] = " + str);
        } else {
            System.out.println("obj is Object = " + obj);
        }
    }

    private static void instanceOfCastingWithGenerics() {
        // one closing '>' is treated as GT token
        Object obj = new HashMap<String, Long[]>();
        if (obj instanceof HashMap<?, ?> map) {
            System.out.println("obj is HashMap<?, ?> = " + map);
        } else {
            System.out.println("obj is Object = " + obj);
        }
    }

    private static void instanceOfCastingWithGenericsWildcard() {
        Object obj = new ArrayList<Object>();
        if (obj instanceof java.util.List<?> map) {
            System.out.println("obj is List<?> = " + map);
        } else {
            System.out.println("obj is Object = " + obj);
        }
    }

    private static void instanceOfInExpressions() {
        Object obj = new Object();
        if (obj != null && obj instanceof Object && !(obj instanceof String str || obj instanceof Integer i)) {
            // note: str and i are not accessible as they're in a branch condition!
            System.out.println("obj is not null and is an Object and not String or Integer");
        }
    }

    /**
     * Generally speaking, if the expression guarantees that whenever the entire expression is true
     * the instanceof part was also evaluated to true, then the type-casted variable is visible inside
     * the scope. In other words, having '||' operator at the level of instanceof breaks it.
     */
    private static void instanceOfCastingVisibilityLimitations() {
        Object obj = new Object();
        // and expressions - 's' is available inside the block
        if (obj instanceof String s && true) {
            System.out.println(s);
        }
        // and expressions - 's' is available inside the block
        if (obj instanceof String s && false) {
            System.out.println(s);
        }
        // or expressions - 's' is not available inside the block
        if (obj instanceof String s || true) {
            //System.out.println(s); does not compile
        }
        // nested expressions - 's' is available inside the block
        if (((obj instanceof String s))) {
            System.out.println(s);
        }
        // nested expressions - 's' is available inside the block
        int j = 0;
        if ((j == 0 || j == 5) && (obj instanceof String s)) {
            System.out.println(s);
        }
        // neither 's' nor 'i' are available in the block
        if (obj instanceof Integer i || obj instanceof String s) {
            //System.out.println(i); does not compile
            //System.out.println(s); does not compile
        }
    }

    private static void detectionOfInstanceOfWithCasting() {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        Object o4 = new Object();

        // no pattern matching - can be branch-instrumented
        if (o1 instanceof String) { }

        // simple type cast - do not instrument
        if (o2 instanceof String s) { }

        // nested type cast - do not instrument
        if (o3 instanceof A.B ab) { }

        // nested type with array - do not instrument
        if (o4 instanceof A.B[] arr) { }
    }

    /*private static int iget(int i) {
        return 0;
    }*/

    /*a simulation how openclover instruments this
    private static void instrumentingInstanceOf() {
        Object obj = new Object();
        if ((((obj instanceof String)&&(iget(7)!=0|true))||(iget(8)==0&false))) {
            String str = (String) obj;
            System.out.println("obj is String = " + str);
        }
    }*/

    /*a simulation how openclover instruments this
    TODO: branch instrumentation of instanceof with casting is unsupported
    private static void instrumentingInstanceOfWithCast() {
        Object obj = new Object();
        if ((((obj instanceof String str)&&(iget(7)!=0|true))||(iget(8)==0&false))) {
            System.out.println("obj is String = " + str); // compilation error - symbol not found!
        }
    }*/

}
