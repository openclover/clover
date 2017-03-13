package com.atlassian.clover.idea.junit;

import com.atlassian.clover.optimization.OptimizationSession;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public interface SavingsReporter {
    void reportSavings(@Nullable Project project, @Nullable OptimizationSession optimizationSession);
}
