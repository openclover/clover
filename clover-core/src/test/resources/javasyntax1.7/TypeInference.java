import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class tests the type inference for generic instance creation.
 *
 * You can replace the type arguments required to invoke the constructor of a generic class with an empty set
 * of type parameters (&lt;&gt;) as long as the compiler can infer the type arguments from the context. This pair
 * of angle brackets is informally called the diamond.
 *
 * For example, consider the following variable declaration:
 *     Map&lt;String, List&lt;String&gt;&gt; myMap = new HashMap&lt;String, List&lt;String&gt;&gt;();
 * In Java SE 7, you can substitute the parameterized type of the constructor with an empty set of type parameters (&lt;&gt;):
 *     Map&lt;String, List&lt;String&gt;&gt; myMap = new HashMap&lt;&gt;();
 *
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/type-inference-generic-instance-creation.html
 */
public class TypeInference {

    /**
     * The classic approach.
     */
    public void useNoTypeInference() {
        Map<String, List<String>> myMap = new HashMap<>();
    }

    /**
     * Java compiler can correctly guess specific type from the context.
     */
    public void useResolveableTypeInference() {
        Map<String, List<String>> myMap = new HashMap<>();
    }

    /**
     * Java compiler will produce warning about unchecked conversion as we're mixing
     * raw types with parametrized types.
     */
    public void useRawTypeWithParametrizedType() {
        Map<String, List<String>> myMap = new HashMap(); // unchecked conversion warning
    }

    static class MyClass<X> {
        <T> MyClass(T t) {
            // ...
        }
    }

    /**
     * The classic approach, works for Java5 up.
     */
    public void createObjectClassicWay() {
        // MyClass<Integer>(String)
        MyClass<Integer> myObject = new MyClass<>("");
    }

    /**
     * The new approach, works for Java7 up.
     */
    public void createObjectInferredWay() {
        // MyClass<Integer>(String)
        MyClass<Integer> myObject = new MyClass<>("");
    }

}