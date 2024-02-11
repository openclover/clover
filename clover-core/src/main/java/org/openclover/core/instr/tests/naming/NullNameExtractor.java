package org.openclover.core.instr.tests.naming;

import org.openclover.core.api.registry.MethodInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Returns 'null' as a name of the test
 */
public class NullNameExtractor implements TestNameExtractor {
    @Override
    @Nullable
    public String getTestNameForMethod(@NotNull MethodInfo methodInfo) {
        return null;
    }
}
