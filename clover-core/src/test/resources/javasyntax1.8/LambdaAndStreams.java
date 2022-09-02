import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test case how lambda functions passed to methods of java.util.stream.Stream are instrumented by Clover See CLOV-1596,
 * CLOV-1463, CLOV-1465
 */
public class LambdaAndStreams {

    private static String notUsedVariable;
    private boolean testLambdaReplaceInBoolean = Arrays.asList("aaa", "bbb", "ccc").stream().filter(e -> e.startsWith("a")).allMatch(e -> e.endsWith("a"));

    public static List<String> testMapAndCollectBounds(List<String> input2) {
        if (Arrays.asList("aaa", "bbb").stream().filter(e -> e.endsWith("b")).noneMatch(e -> "aaa".equals(""))) {
            notUsedVariable = "none match";
        }

        return input2.stream()
                .map(e ->
                        e.toUpperCase())
                .collect(Collectors.toList());
    }

    public static Stream<String> testMapAndFilterBounds(List<String> input3) {
        return input3.stream()
                .map(e -> e.toUpperCase())
                .filter(e -> !e.isEmpty());
    }

//    public static Stream<String> testMapAndFilterBoundsAndMethodReference(List<String> input3) {
//        return input3.stream()
//                .map(String::toUpperCase) //TODO CLOV-1762: Compilation fails
//                .filter(e -> !e.isEmpty());
//    }


    public static void testLambdaReplaceInConstructorArg() {
        new MyPredicate(Arrays.asList("aaa", "bbb", "ccc")
                .stream()
                .filter(
                        e ->
                                e.startsWith("a")
                )
                .collect(Collectors.counting())).test(1L);

        new MyLambdaConstructorPredicate((a) -> 29 * a).test(1L);
    }

    public static void testDifferentNestedLambdas() {
        Arrays.asList("abc", "def", "").stream().filter(a -> a.matches("[abc]+"));
        Callable<Runnable> run = () -> () -> System.out.println("hello World");
        Callable<Runnable> run2 = () -> () -> Arrays.asList(Arrays.asList("a1", "a1"), Arrays.asList("b1", "b2")).stream().flatMap(el -> el.stream());
    }

    public static void generate(Stream<Double> firstStream, Stream<Double> secondStream) {
        secondStream = secondStream.limit(10L).onClose(() -> System.out.println("Closed"));
        Stream.concat(firstStream.limit(20), secondStream).max((f, s) -> f.compareTo(s));
    }

    public static void testStreamsWithTypeCast() {
        List<String> list = new ArrayList<>();
        list.stream().map((TypeOne) (a) -> a).collect(Collectors.toList());
    }

    public static void testNotStreamMapMethod() {
        MapClass mapClass = new MapClass();
        mapClass.map((Map<String, String> internalMap) ->/*CLOVER:VOID*/ internalMap.put("some", "some"));
    }

    private static void testToArray() {
        final Object[] r = Stream.of(1, 2, 3, 4, 5, 6).map((i) -> "String:" + String.valueOf(i)).toArray();
        final String[] ints = Stream.of(1, 2, 3, 4, 5, 6).toArray(size -> new String[size]);
    }

    public static void main(String[] args) {
        testMapAndCollectBounds(Arrays.asList("abc", "def", ""));
        testMapAndFilterBounds(Arrays.asList("abc", "def", ""));
        testNotStreamMapMethod();
        testLambdaReplaceInConstructorArg();
        testDifferentNestedLambdas();
        testToArray();
        LambdaAndStreams.generate(Stream.generate(() -> Math.random() - 1), Stream.generate(() -> Math.random() + 1));

        new LambdaAndStreams();
    }


    private static final class MapClass {

        private Map<String, String> internalMap = new HashMap<>();

        public void map(Effect<Map<String, String>> obj) {
            obj.apply(internalMap);
        }
    }

    @FunctionalInterface
    private interface Effect<T> {
        void apply(T obj);
    }

    static class MyPredicate implements Predicate<Long> {

        private final Long no;

        MyPredicate(Long no) {
            this.no = no;
        }

        @Override
        public boolean test(Long aLong) {
            return no.equals(aLong);
        }
    }

    static class MyLambdaConstructorPredicate implements Predicate<Long> {

        private final Function<Long, Long> no;


        MyLambdaConstructorPredicate(Function<Long, Long> no) {
            this.no = no;
        }

        @Override
        public boolean test(Long aLong) {
            return no.apply(aLong).equals(aLong);
        }
    }


    interface TypeOne extends Function<String, String> {
    }
}
