import java.io.Serializable;
import java.util.ArrayList;

/**
 * Examples of constructor references.
 */
public class LambdaAndConstructorReferences {


    void foo() {
        Produce<ArrayList<String>> createStringList = ArrayList<String>::new;
    }

    interface Produce<T> {
        T produce();
    }

    interface ProduceRaw {
        Object produce();
    }

    interface ProduceString {
        String[] produce(int n);
    }

    static class One<S> {
        static class Two<T> {

        }

        Three<S> three = new Three<S>();

        class Three<U> {
            public U hi() { return null; }
        }
    }

    /**
     * Test constructor of the simple type (i.e. non-generic)
     */
    public void testObjectTypeReference() {
        Produce<String> createString = String::new;
        String string = createString.produce();
        System.out.println("testObjectTypeReference: string=[" + string + "] class=[" + string.getClass()+ "]");
    }

    interface Map<T, S> {
        T map(S x);
    }

    /**
     * Test constructor of an array.
     */
    public void testArrayReference() {
        ProduceString createStringArray = String[]::new;
        String[] stringArray = createStringArray.produce(10);
        System.out.println("testArrayReference: stringArray.length=[" + stringArray.length + "] class=[" + stringArray.getClass()+ "]");
    }

    /**
     * Test constructor of a generic type, but using raw reference. It involves type inference in javac.
     */
    public void testRawTypeReference() {
        ProduceRaw createRawList = ArrayList::new;
        Object rawList = createRawList.produce();
        System.out.println("testConstructorReference: rawList=" + rawList + " class=" + rawList.getClass());
    }

    /**
     * Test constructor of a generic type.
     */
    public void testGenericTypeReference() {
        // Note that we cannot use raw type with a generic constructor "ArrayList::<String>new".
        //
        // You have to either use a simple form of "ArrayList::new" (which will trigger the type inference) or
        // provide the full type information like "ArrayList<String>::new" or "ArrayList<String>::<String>new"
        //
        // See:
        //   https://bugs.openjdk.java.net/browse/JDK-8023549
        //   https://bugs.openjdk.java.net/browse/JDK-8027798
        //   https://bugs.openjdk.java.net/browse/JDK-8030356
        Produce<ArrayList<String>> createStringList = ArrayList<String>::new;
        Produce<ArrayList<String>> createStringList2 = ArrayList<String>::<String>new;
        ArrayList<String> stringList = createStringList.produce();
        System.out.println("testConstructorReference: stringList=" + stringList + " class=" + stringList.getClass());

        // you can nest classes of course
        Produce<One.Two<Integer>> oneTwo = One.Two<Integer>::new;

        // and use other qualifiers like fields, array indexes etc
        One<String>[] ones = new One[10];
        ones[0] = new One<String>();
        Produce<String> oneThreeHi = ones[3 - 2 - 1].three::<Integer>hi;
    }

    /**
     * Example how we can use a type cast with a method reference
     */
    public void testReferenceWithTypeCast() {
        // constructor of a simple type with a cast
        Object oo = (Produce<String>)String::new;
        String s = ((Produce<String>)oo).produce();
        System.out.println("testReferenceWithTypeCast: s=[" + s + "]");

        // constructor of a generic type from a nested class with a cast to multiple interfaces
        Produce<One.Two<Integer>> oneTwoCast = (Produce<One.Two<Integer>> & Serializable)One.Two<Integer>::new;
        System.out.println("testReferenceWithTypeCast: oneTwoCast=[" + oneTwoCast + "]");
    }

    public static void main(String[] args) {
        LambdaAndConstructorReferences lam = new LambdaAndConstructorReferences();
        lam.testObjectTypeReference();
        lam.testArrayReference();
        lam.testRawTypeReference();
        lam.testGenericTypeReference();
        lam.testReferenceWithTypeCast();
    }

}
