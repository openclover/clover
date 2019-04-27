package junit5.parameterized;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * Execute JUnit test runner without Clover's test listener. Clover can still collect code coverage, but test names do
 * not have iteration numbers (parameterized tests).
 */
public class RunJunit5ParameterizedWithoutClover {

    public static void main(String[] args) {
        runTests();
    }


    private static void runTests() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(PersonTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);
        launcher.execute(request);
    }
}
