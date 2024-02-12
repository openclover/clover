package org.openclover.core.instr.tests.naming;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.MethodInfo;

/**
 * Extracts a test name from the model for a given method.
 */
public interface TestNameExtractor {

    /**
     * Returns name of the test corresponding with this test method or <code>null</code> if such test name could not be
     * determined.
     *
     * @param methodInfo test method for which we look for a test name
     * @return String test name or <code>null</code> if not found
     */
    @Nullable
    String getTestNameForMethod(@NotNull MethodInfo methodInfo);
}
