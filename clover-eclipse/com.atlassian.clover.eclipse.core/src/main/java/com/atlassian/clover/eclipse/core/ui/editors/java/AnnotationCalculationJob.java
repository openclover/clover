package com.atlassian.clover.eclipse.core.ui.editors.java;

import com.atlassian.clover.eclipse.core.SystemJob;

public abstract class AnnotationCalculationJob extends SystemJob {
    public AnnotationCalculationJob(AnnotationUpdateRule schedulingRule) {
        super("Recalculating coverage annotations");
        setRule(schedulingRule);
    }
}
