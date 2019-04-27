package junit5.parameterized;

import com.atlassian.clover.recorder.junit.CloverJunit5TestExecutionListener;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * Execute JUnit runner with Clover's test listener. Thanks to this, Clover can read the exact name of the test at
 * runtime.
 */
public class RunJunit5ParameterizedWithClover {

    public static void main(String[] args) {
        runTests();
    }


    private static void runTests() {
        CloverJunit5TestExecutionListener cloverTestListener = new CloverJunit5TestExecutionListener();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(PersonTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);
        launcher.registerTestExecutionListeners(cloverTestListener);
        launcher.execute(request);
    }
}
