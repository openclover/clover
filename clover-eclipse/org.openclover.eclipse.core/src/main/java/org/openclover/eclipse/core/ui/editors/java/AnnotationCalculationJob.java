package org.openclover.eclipse.core.ui.editors.java;

import org.openclover.eclipse.core.SystemJob;

public abstract class AnnotationCalculationJob extends SystemJob {
    public AnnotationCalculationJob(AnnotationUpdateRule schedulingRule) {
        super("Recalculating coverage annotations");
        setRule(schedulingRule);
    }
}
