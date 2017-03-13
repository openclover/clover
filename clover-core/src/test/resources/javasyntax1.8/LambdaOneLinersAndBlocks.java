/**
 * Sample showing lambdas with:
 *  - single expressions (returning values or not)
 *  - multiple statements (returning values or not)
 *
 * Notes:
 *  - the body may be a block (surrounded by braces) or an expression
 *  - a block body can return a value (value-compatible) or nothing (void-compatible)
 *  - the rules for using or omitting the return keyword in a block body are the same as those for an ordinary method body
 *  - if the body is an expression, it may return a value or nothing
 *
 * JEP126 - http://openjdk.java.net/jeps/126
 */
public class LambdaOneLinersAndBlocks {

    interface Execute {
        void execute();
    }

    interface Produce<T> {
        T produce();
    }

    public void lambdaWithOneExpressionReturningVoid() {
        Execute sayHello = () -> System.out.print("Hello");
    }

    public void lambdaWithOneExpressionReturningInteger() {
        Produce<Integer> getInt = () -> 777;
        for (int i = 0; i < 10; i++)
            getInt.produce();
    }

    private Integer throwRuntimeException() {
        throw new RuntimeException();
    }

    /**
     * Test whether exceptions thrown by methods called by a proxy are propagated correctly.
     */
    public void lambdaWithOneExpressionThrowingException() {
        try {
            Produce<Integer> getSomething = () -> throwRuntimeException();
            getSomething.produce();
        } catch (Throwable ex) {
            System.out.print("Exception caught: " + ex.getClass().getName() + "\n");
        }
    }

    public void lambdaWithBlockReturningVoid() {
        Execute sayHello = () -> { System.out.print("Hello"); System.out.print("Hello"); System.out.print("Hello"); };
        sayHello.execute();
    }

    public void lambdaWithEmptyBlock() {
        Execute sayHello = () -> { };
        sayHello.execute();
    }

    public void lambdaWithBlockReturningInteger() {
        Produce<Integer> getInt = () -> { int i = 777; i += 777; return i; };
        getInt.produce();
    }

    public void lambdaWithBlockAndNestedBlockReturningInteger() {
        Produce<Integer> getInt = () -> {  { int i = 777; } return 777; };
        getInt.produce();
    }

    public static void main(String[] args) {
        LambdaOneLinersAndBlocks lam = new LambdaOneLinersAndBlocks();
        lam.lambdaWithOneExpressionReturningVoid();
        lam.lambdaWithOneExpressionReturningInteger();
        lam.lambdaWithOneExpressionThrowingException();
        lam.lambdaWithBlockReturningVoid();
        lam.lambdaWithEmptyBlock();
        lam.lambdaWithBlockReturningInteger();
        lam.lambdaWithBlockAndNestedBlockReturningInteger();
    }

}
