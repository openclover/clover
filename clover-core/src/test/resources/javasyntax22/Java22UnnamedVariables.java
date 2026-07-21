/**
 * Unnamed Variables (finalized in Java 22).
 * The underscore '_' as an unnamed local, resource, catch, lambda and for-loop variable.
 * These positions have always lexed '_' as an IDENT, so they need no grammar change - the
 * fixture locks the behaviour and proves the instrumented source still compiles and runs.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Java22UnnamedVariables {

    static int sideEffect() {
        return 42;
    }

    public static void main(String[] args) {
        // unnamed local variable
        int _ = sideEffect();

        // unnamed for-loop variable
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        int total = 0;
        for (var _ : list) {
            total++;
        }
        System.out.println("total = " + total);

        // unnamed catch variable
        try {
            Integer.parseInt("not a number");
        } catch (NumberFormatException _) {
            System.out.println("caught = ok");
        }

        // unnamed lambda parameter
        BiFunction<Integer, Integer, Integer> firstOnly = (a, _) -> a;
        System.out.println("firstOnly = " + firstOnly.apply(7, 9));
    }
}
