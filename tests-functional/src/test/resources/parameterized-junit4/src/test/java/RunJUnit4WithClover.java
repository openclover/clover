import org.junit.runner.JUnitCore;
import org.openclover.runtime.recorder.junit.JUnitTestRunnerInterceptor;

/**
 * Execute JUnit runner with Clover's test listener.
 * Thanks to this, Clover can read the exact name of the test at runtime.
 */
public class RunJUnit4WithClover {
    public static void main(String[] args) {
        JUnitCore core= new JUnitCore();
        core.addListener(new JUnitTestRunnerInterceptor());
        core.run(PersonTest.class);
        core.run(SquareTest.class);
    }
}
