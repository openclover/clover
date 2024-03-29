package org.openclover.eclipse.testopt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.openclover.core.optimization.Snapshot;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.runtime.api.CloverException;

import java.io.IOException;

public class GenerateSnapshotDelegate extends AbstractJavaLaunchConfigurationDelegate {
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        final IJavaProject project = verifyJavaProject(configuration);
        final CloverProject cp = CloverProject.getFor(project);
        final String initString = cp.getSettings().getInitString();
        try {
            Snapshot.generateFor(initString).store();
        } catch (IOException e) {
            TestOptimizationPlugin.logWarning("Error storing created snapshot file", e);
        } catch (CloverException e) {
            TestOptimizationPlugin.logWarning("Error loading OpenClover database", e);
        }
    }
}
