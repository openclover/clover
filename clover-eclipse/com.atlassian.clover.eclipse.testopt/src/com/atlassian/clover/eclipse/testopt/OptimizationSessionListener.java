package com.atlassian.clover.eclipse.testopt;

import com.atlassian.clover.optimization.OptimizationSession;

public interface OptimizationSessionListener {
    void sessionFinished(OptimizationSession session);
}
