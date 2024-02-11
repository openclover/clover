package org.openclover.core.instr.tests.naming;

import org.openclover.core.api.registry.MethodInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Returns a name of the test exactly the same as a name of the method implementing it.
 */
public class PlainMethodNameExtractor implements TestNameExtractor {
    @Override
    @Nullable
    public String getTestNameForMethod(@NotNull MethodInfo methodInfo) {
        return methodInfo.getSimpleName();
    }
}
