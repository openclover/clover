/**
 * Ensure that a class which has no methods, but still contains executable code in a form of lambda functions
 * assigned to class' fields will be instrumented by Clover and will contain coverage recorder static field.
 */
public class LambdaAndTest {
    Runnable r = System::currentTimeMillis;
}
