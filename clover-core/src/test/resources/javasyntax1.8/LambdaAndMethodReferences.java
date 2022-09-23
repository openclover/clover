import java.util.ArrayList;
import java.util.Arrays;

/**
 * Sample showing how we can use instance method references, static method references as well as constructor
 * calls as lambda expressions.
 *
 * JEP126 - http://openjdk.java.net/jeps/126
 */
public class LambdaAndMethodReferences {

    interface Produce<T> {
        T produce();
    }

    interface ProduceRaw {
        Object produce();
    }

    interface Map<T> {
        T map(T x);
    }

    static String transform(Produce<String> lambda) {
        return lambda.produce();
    }

    /**
     * Example of a lambda expression when we refer to a method call of some object instance. There are two variants
     * of the syntax:
     *  1. reference method of an explicit object instance
     *  2. reference method by it's class type, which is later being replaced by calls of the method on specific
     *     instances
     */
    public void testInstanceMethodReference() {
        // referencing by 'instance::methodName'
        String myString = "Hello World";
        transform(myString::toLowerCase);

        // referencing by 'Class::methodName', which is being translated to
        // '(Class obj) -> { obj.methodName(); }' by the compiler
        ArrayList<String> myStringList = new ArrayList<>();
        myStringList.forEach(String::toLowerCase);
    }

    /**
     * Example how we can refer to a static method.
     */
    public void testStaticMethodReference() {
        // reference to a non-argument static method
        Runnable callGc = System::gc;
        callGc.run();

        // reference to a static method with some arguments, comparison of signature types is performed to match
        // Integer.compare(int, int) matches the Comparator.compare(int, int); autoboxing occurs
        Integer[] myArray = { 3, 2, 1 };
        Arrays.sort(myArray, Integer::compare);
    }

    /**
     * Example how we can use a type cast with a method reference
     */
    public void testMethodReferenceWithTypeCast() {
        Object oo = (Produce<String>)String::new;
        String s = ((Produce<String>)oo).produce();
        System.out.println("testMethodReferenceWithTypeCast: s=[" + s + "]");
    }

    public static void main(String[] args) {
        LambdaAndMethodReferences lam = new LambdaAndMethodReferences();
        lam.testInstanceMethodReference();
        lam.testStaticMethodReference();
        lam.testMethodReferenceWithTypeCast();
    }

}
