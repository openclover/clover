/**
 * Sample showing possible combination of lambdas with:
 *  - explicit argument and return types
 *  - argument and return types inferred from context
 *  - single implicit argument without parentheses ()
 *  - no arguments or no return values
 *
 * Notes:
 *  - parameter types may be explicitly declared or implicitly inferred
 *  - declared- and inferred-type parameters may not be mixed in a single lambda expression
 *  - parentheses may be omitted for a single inferred-type parameter
 *
 * JEP126 - http://openjdk.java.net/jeps/126
 */
public class LambdaArgumentAndReturnTypes {
    interface Join<T> {
        T join(T x, T y);
    }

    interface Map<T> {
        T map(T x);
    }

    interface Produce<T> {
        T produce();
    }

    interface Consume<T> {
        void consume(T x);
    }

    interface Execute {
        void execute();
    }

    /**
     * - explicit arguments types
     * - implicit return
     * - expression-like statement
     */
    public void lambda_TypedArguments_ImplicitReturn() {
        Join<Integer> sumIntegers = (Integer x, Integer y) -> x + y;
        sumIntegers.join(10, 20);
    }

    /**
     * - implicit arguments types
     * - implicit return
     * - expression-like statement
     */
    public void lambda_ImplicitArguments_ImplicitReturn() {
        Join<Integer> sumGuessWhat = (x, y) -> x + y;
        sumGuessWhat.join(30, 40);
    }

    /**
     * - no arguments
     * - void return
     * - expression-like statement
     */
    public void lambda_NoArguments_VoidReturn() {
        Execute printHello = () -> System.out.println("Hello World");
        printHello.execute();
    }

    /**
     * - no arguments
     * - implicit return type
     * - expression-like statement
     */
    public void lambda_NoArguments_ImplicitReturn() {
        Produce<String> produce = () -> "created";
        produce.produce();
    }

    /**
     * - explicit argument type
     * - single argument in parentheses
     * - void return
     * - expression-like statement
     */
    public void lambda_TypedArgument_VoidReturn() {
        Consume<String> printString = (String s) -> System.out.println(s);
        printString.consume("abc");
    }

    /**
     * - implicit argument type
     * - single argument in parentheses
     * - void return
     * - expression-like statement
     */
    public void lambda_ImplicitArgument_VoidReturn() {
        Consume<String> printString = (s) -> System.out.println(s);
        printString.consume("abc");
    }

    /**
     * - implicit argument type
     * - single argument not enclosed in parentheses
     * - void return
     * - expression-like statement
     */
    public void lambda_ImplicitArgument_NoParentheses_VoidReturn() {
        Consume<String> printString = s -> System.out.println(s);
        printString.consume("abc");
    }

    /**
     * - implicit argument type
     * - single argument in parentheses
     * - implicit return type
     * - expression-like statement
     */
    public void lambda_ImplicitArgument_ImplicitReturn() {
        Map<Double> multiply = (x) -> 2 * x;
        multiply.map(100.0);
    }

    /**
     * - implicit argument type
     * - single argument not enclosed in parentheses
     * - implicit return type
     * - expression-like statement
     */
    public void lambda_ImplicitArgument_NoParentheses_ImplicitReturn() {
        Map<Double> multiply = x -> 3 * x;
        multiply.map(100.0);
    }

    public static void main(String[] args) {
        LambdaArgumentAndReturnTypes lam = new LambdaArgumentAndReturnTypes();
        lam.lambda_TypedArguments_ImplicitReturn();
        lam.lambda_ImplicitArguments_ImplicitReturn();
        lam.lambda_NoArguments_VoidReturn();
        lam.lambda_NoArguments_ImplicitReturn();
        lam.lambda_TypedArgument_VoidReturn();
        lam.lambda_ImplicitArgument_VoidReturn();
        lam.lambda_ImplicitArgument_NoParentheses_VoidReturn();
        lam.lambda_ImplicitArgument_ImplicitReturn();
        lam.lambda_ImplicitArgument_NoParentheses_ImplicitReturn();
    }
}
