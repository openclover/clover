import java.util.ArrayList;
import java.util.List;

/**
 * Tests how usage of @SafeVarargs will affect compiler warnings.
 * We expect no warning at method declaration and no warning at method call.
 *
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/non-reifiable-varargs.html
 */
public class NonReifiableTypesSafeVarargs {

    /**
     * Compilation of this method should not produce any warning.
     */
    @SafeVarargs
    public static <T> void addToList3 (List<T> listArg, T... elements) {
        for (T x : elements) {
            listArg.add(x);
        }
    }


    public static void main() {
        List<String> stringListA = new ArrayList<>();
        List<String> stringListB = new ArrayList<>();
        List<List<String>> listOfStringLists = new ArrayList<>();

        // Compilation of this line should not produce any warning
        addToList3(listOfStringLists, stringListA, stringListB);
    }
}