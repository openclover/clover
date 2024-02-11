package org.openclover.eclipse.testopt;

import org.openclover.core.optimization.OptimizationSession;

public interface OptimizationSessionListener {
    void sessionFinished(OptimizationSession session);
}
