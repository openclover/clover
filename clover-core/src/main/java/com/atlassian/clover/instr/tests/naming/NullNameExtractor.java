package com.atlassian.clover.instr.tests.naming;

import com.atlassian.clover.api.registry.MethodInfo;
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
