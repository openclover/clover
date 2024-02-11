package org.openclover.core.instr.tests.naming;

import org.openclover.core.api.registry.MethodInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

/**
 * Standard implementation of the "combined" test extractor supporting:
 *  - Spock framework features
 *  - JUnit4 parameterized tests
 *  - plain methods
 */
public class DefaultTestNameExtractor implements TestNameExtractor {

    public static final TestNameExtractor INSTANCE = new DefaultTestNameExtractor();

    private static final List<TestNameExtractor> TEST_EXTRACTORS = newArrayList(
            new JUnitParameterizedTestExtractor(),
            new SpockFeatureNameExtractor(),
            new NullNameExtractor());

    @Override
    @Nullable
    public String getTestNameForMethod(@NotNull MethodInfo methodInfo) {
        for (final TestNameExtractor extractor : TEST_EXTRACTORS) {
            final String testName = extractor.getTestNameForMethod(methodInfo);
            if (testName != null) {
                return testName;
            }
        }
        return null;
    }
}
