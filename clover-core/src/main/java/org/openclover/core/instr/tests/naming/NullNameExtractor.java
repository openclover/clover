package org.openclover.core.instr.tests.naming;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.MethodInfo;

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
