package org.openclover.idea.junit;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.optimization.OptimizationSession;

public interface SavingsReporter {
    void reportSavings(@Nullable Project project, @Nullable OptimizationSession optimizationSession);
}
