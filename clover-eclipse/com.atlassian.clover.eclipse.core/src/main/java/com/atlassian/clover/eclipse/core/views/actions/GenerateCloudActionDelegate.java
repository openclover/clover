package com.atlassian.clover.eclipse.core.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;

public abstract class GenerateCloudActionDelegate extends GenerateReportletActionDelegate {
    @Override
    public void run(IAction action) {
        createCloudJob((IProject) projects.iterator().next()).schedule();
    }
    
    protected abstract GenerateCloudJob createCloudJob(IProject project);
}
