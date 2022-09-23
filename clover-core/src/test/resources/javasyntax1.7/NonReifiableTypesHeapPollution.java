import java.util.*;

/**
 * Tests how usage of vargargs method with template parameters will result in compiler warnings.
 *
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/non-reifiable-varargs.html
 */
public class NonReifiableTypesHeapPollution {

    /**
     * Compilation of this method should produce heap pollution warning like:
     * [unchecked] Possible heap pollution from parameterized vararg type T
     */
    public static <T> void addToList (List<T> listArg, T... elements) {
        for (T x : elements) {
            listArg.add(x);
        }
    }

    /**
     * Compilation of this code should produce heap pollution warning like below.
     */
    public void heapPollution() {
        List<String> stringListA = new ArrayList<>();
        List<String> stringListB = new ArrayList<>();
        List<List<String>> listOfStringLists = new ArrayList<>();

        // Compilation of this line should produce warning like:
        // [unchecked] unchecked generic array creation for varargs parameter of type List<String>[]
        addToList(listOfStringLists, stringListA, stringListB);
    }
}