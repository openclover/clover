package org.openclover.eclipse.core.ui.editors.java;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public final class AnnotationUpdateRule implements ISchedulingRule {
    @Override
    public boolean contains(ISchedulingRule schedulingRule) {
        return schedulingRule == this;
    }

    @Override
    public boolean isConflicting(ISchedulingRule schedulingRule) {
        return schedulingRule == this;
    }
}
