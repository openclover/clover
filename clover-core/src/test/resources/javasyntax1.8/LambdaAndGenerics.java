public class LambdaAndGenerics {
    static interface Predicate<T> {
        boolean test(T t);
    }

    /**
     * Works fine for N, M declared for a class
     */
    static class DoesNotFail<M, N> {
        void start() {
            System.out.println(foo(e -> true));              // compiler works-out types correctly with our lambdaInc()
            System.out.println(foo(e -> { return true; })); // no lambdaInc()
        }
        String foo(Class<N> arg) {
            return "DoesNotFail class: " + arg;
        }
        String foo(Predicate<M> arg) {
            return "DoesNotFail predicate: " + arg.test(null);
        }
    }
    /**
     * Fails when N, M are declared for methods
     */
    static class Fails {
        void start() {
            System.out.println(goo(e -> false)); // COMPILER ERRORS "reference to one is ambiguous" + "cannot infer type variables"
        }
        <N> String goo(Class<N> arg) {
            return "Fails class: " + arg;
        }
        <M> String goo(Predicate<M> arg) {
            return "Fails predicate: " + arg.test(null);
        }
    }

    public static void main(String[] args) {
        new DoesNotFail<String, Integer>().start();
        new Fails().start();
    }
}
