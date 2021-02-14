package org.openclover.eclipse.testopt;

import com.atlassian.clover.api.CloverException;
import org.openclover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.optimization.Snapshot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;

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
            TestOptimizationPlugin.logWarning("Error loading Clover database", e);
        }
    }
}
