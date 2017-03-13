
import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Test case showing various places in which a lambda expression can be used
 *  - variable declarations and assignments and array initializers
 *  - return statements
 *  - method or constructor arguments
 *  - lambda expression bodies (i.e. lambda in lambda)
 *  - ternary conditional expressions (?:)
 *  - cast expressions, which provide the target type explicitly
 * plus:
 *  - recursive calls of the lambda itself
 *
 * JEP126 - http://openjdk.java.net/jeps/126
 */
public class LambdaInContexts {

    // set of functional interfaces, using different visibility modifiers to test whether lambdaInc() proxy will be able
    // to intercept all of them

    public interface Map<T> {
        T map(T x);
    }

    interface Produce<T> {
        T produce(int i);
    }

    protected interface Produce2<T> {
        T produce(T i, T[] j);
    }

    private interface Printer<T> {
        T print();
    }

    /**
     * Variable declarations and assignments and array initializers, for which the target type is the type
     * (or the array type) being assigned to;
     */
    public void lambdaInVariableDeclarationAndAssignment() {
        Map<Integer> duplicate = x -> 2 * x;       // assign lambda to a variable

        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += duplicate.map(i);               // call lambda
        }
        assert sum == 110;

        final Integer[] table10 = produceN(Integer[]::new, 10);           // method/constructor reference as lambda
        assert table10.length == 10;

        final Integer[] table100 = produceN(n -> { return new Integer[100 * n]; }, 1); // array construction
        assert table100.length == 100;
    }

    private Integer[] produceN(Produce<Integer[]> p, int size) {
        return p.produce(size);
    }

    /**
     * Return statements, for which the target type is the return type of the method;
     * @return Map<Integer>
     */
    public Map<Integer> lambdaInReturnStatement() {
        return x -> 2 * x;
    }

    /**
     * Method or constructor arguments, for which the target type is the type of the appropriate parameter.
     * If the method or constructor is overloaded, the usual mechanisms of overload resolution are used before
     * the lambda expression is matched to the target type. (After overload resolution, there may still be more
     * than one matching method or constructor signature accepting different functional interfaces with identical
     * functional descriptors. In this case, the lambda expression must be cast to the type of one of these
     * functional interfaces).
     */
    public void lambdaAsMethodArgument() {
        // Printer<String>
        print( () -> { String out = ""; return out; });

        // Printer<Integer>
        print( () -> 100 );

        // Printer<Object>
        print(Object::new);
    }

    private void print(Printer input) {
        System.out.println("print(Printer) called: input type=" + input.getClass().toString() + " value=" + input.print());
    }

    /**
     * Lambda expression bodies, for which the target type is the type expected for the body, which is derived in
     * turn from the outer target type.
     *
     * Callable<Runnable> has "Runnable call() throws Exception;" and the
     * Runnable has "void run()"
     */
    public void lambdaInLambda() {
        try {
            Callable<Runnable> call = () -> () -> { System.out.println("Callable calls Runnable which calls run"); };
            System.out.println("lambdaInLambda " + call.call());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ternary conditional expressions (?:), for which the target type for both arms is provided by the context.
     */
    public void lambdaInTernaryExpression(boolean flag) {
        try {
            // we can embed lambdas in ternary expression
            Callable<Integer> c1 = flag ? () -> 23 : () -> 42;

            // lambdas can be also mixed with instantiation of the class implementing the same interface as lambda
            Callable<Integer> c2 = flag ? () -> 23 : new Callable<Integer>() {
                public Integer call() throws Exception {
                    return Integer.valueOf(0);
                }
            };

            System.out.println("lambdaInTernaryExpression c1" + c1.call());
            System.out.println("lambdaInTernaryExpression c2" + c2.call());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * We can use class cast expression to specify lambda.
     */
    public void lambdaWithCastExpression() {
        // Illegal: could be Runnable or Callable
        // Object o = () -> { System.out.println("hi"); };

        // Cast a lambda; now it's legal because disambiguated
        Object o = (Runnable) () -> { System.out.println("lambdaWithCastExpression cast a lambda"); };
        ((Runnable)o).run();

        // Cast a lambda; copy (Runnable) inside lambdaInc to make type inference working
        Object oo = (Runnable) () -> System.out.println("lambdaWithCastExpression cast a lambda one-liner");
        ((Runnable)oo).run();

        // Cast a lambda to several interfaces at once
        Object o2 = (Runnable & Serializable) () -> { System.out.println("lambdaWithCastExpression cast with serializable"); };
        ((Runnable)o2).run();

        // Cast a lambda to several interfaces at once; copy the (Runnable & Serializable) inside lambdaInc
        Object oo2 = (Runnable & Serializable) () -> System.out.println("lambdaWithCastExpression one-liner cast with serializable");
        ((Runnable)oo2).run();
    }


    /**
     * Lambda expressions can be used to define recursive functions, provided that the recursive call uses a
     * name defined in the enclosing environment of the lambda. This means that recursive definitions can only
     * be made in the context of variable assignment and, in fact given the assignment-before-use rule for
     * local variables only of instance or static variable assignment.
     */
    public void lambdaRecursion() {
        System.out.println("Power of 10 is " + factorial.map(10));
    }

    /**
     * Test at which priority lambdas are being processed. They can be used in variable assignments,
     * and ternary expressions, but not with logical or arithmetic operators. Inside lambda we can have any
     * expression, including the variable assignment (i.e. level 13 in java.g) which is being returned.
     * @param z
     */
    public void lambdaVsOperatorPriority(int z) {
        // z != 0
        //    ? (y) -> { return (y < 0 ? y+1 : y-1) }
        //    : (y) -> { return 3 * y }
        Produce<Integer> o = z != 0 ? y -> y < 0 ? y + 1 : y - 1 : y -> 3 * y;
        System.out.println("lambdaVsOperatorPriority z=" + z + " y=10 o=" + o.produce(10));
        System.out.println("lambdaVsOperatorPriority z=" + z + " y=20 o=" + o.produce(20));

        Produce<Integer> oo = (z != 0)
            ? (y) -> { return (y < 0 ? y + 1 : y - 1); }
            : (y) -> { return 3 * y; };
        System.out.println("lambdaVsOperatorPriority z=" + z + " y=10 oo=" + oo.produce(10));
        System.out.println("lambdaVsOperatorPriority z=" + z + " y=20 oo=" + oo.produce(20));

        // return value of the assignment in lambda
        Produce2<Integer> ooo = (y, zzz) -> zzz[0] = y;
        Integer[] out = new Integer[1];
        Integer in = Integer.valueOf(10);
        Integer ret = ooo.produce(Integer.valueOf(10), out);
        System.out.println("lambdaVsOperatorPriority zzz[0]=" + out[0] + " ret=" + ret);
        assert out[0] == in && ret == in;
    }

    // We cannot use "i * factorial.map(i - 1)" due to the "JLS 8.3.2.3" rule. Self-reference in initializer can occur
    // under specific conditions only. See the https://bugs.openjdk.java.net/browse/JDK-8027941 for details.
    static Map<Integer> factorial = i -> i == 0 ? 1 : i * LambdaInContexts.factorial.map(i - 1);

    public static void main(String[] args) {
        LambdaInContexts lam = new LambdaInContexts();
        lam.lambdaInVariableDeclarationAndAssignment();
        lam.lambdaInReturnStatement();
        lam.lambdaAsMethodArgument();
        lam.lambdaInLambda();
        lam.lambdaInTernaryExpression(false);
        lam.lambdaWithCastExpression();
        lam.lambdaRecursion();
        lam.lambdaVsOperatorPriority(0);
    }
}
