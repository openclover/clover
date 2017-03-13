import java.util.ArrayList;
import java.util.List;

/**
 * Tests how usage of @SuppressWarnings({"unchecked", "varargs"}) will affect complier warnings.
 * We expect no warning at method declaration, but one warning at method call.
 *
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/non-reifiable-varargs.html
 */
public class NonReifiableTypesSuppressWarnings {
    /**
     * Compilation of this method should not produce any warning.
     * @param listArg
     * @param elements
     * @param <T>
     */
    @SuppressWarnings({"unchecked", "varargs"})
    public static <T> void addToList2 (List<T> listArg, T... elements) {
        for (T x : elements) {
            listArg.add(x);
        }
    }

    public static void main() {
        List<String> stringListA = new ArrayList<String>();
        List<String> stringListB = new ArrayList<String>();
        List<List<String>> listOfStringLists = new ArrayList<List<String>>();

        // Compilation of this line should produce warning like:
        // [unchecked] unchecked generic array creation for varargs parameter of type List<String>[]
        addToList2(listOfStringLists, stringListA, stringListB);
    }
}