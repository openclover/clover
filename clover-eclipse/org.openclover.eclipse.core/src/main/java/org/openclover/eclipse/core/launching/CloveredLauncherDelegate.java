package org.openclover.eclipse.core.launching;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.builder.InstrumentationProjectPathMap;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import java.util.List;
import java.util.Set;

import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Sets.newHashSet;

public abstract class CloveredLauncherDelegate
    implements ILaunchConfigurationDelegate2, IExecutableExtension {
    private ILaunchConfigurationDelegate launchdelegate;
    private ILaunchConfigurationDelegate2 launchdelegate2;

    @Override
    public void setInitializationData(
        IConfigurationElement config, String propertyName, Object data) throws CoreException {
        launchdelegate = launchDelegateFor(config.getAttribute(LaunchingConstants.TYPE_CONFIG_ATTRIBUTE));
        launchdelegate2 = launchdelegate instanceof ILaunchConfigurationDelegate2 ? (ILaunchConfigurationDelegate2) launchdelegate : null;
    }

    private ILaunchConfigurationDelegate launchDelegateFor(String launchType) throws CoreException {
        ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(launchType);

        if (type == null) {
            throw CloverPlugin.logAndThrowError("Unknown launch type " + launchType);
        } else {
            // TODO: we may have multiple delegates, we're picking up a first one right now
            ILaunchDelegate[] delegates = type.getDelegates(newHashSet(ILaunchManager.RUN_MODE));
            if (delegates.length > 0) {
                return delegates[0].getDelegate();
            } else {
                return null;
            }
        }
    }

    @Override
    public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("Launching " + config.getName(), 1);

        if (!monitor.isCanceled()) {
            ILaunchConfigurationWorkingCopy configurationWorkingCopy =
                bindCustomOutputDirToLaunchClasspath(
                    bindCloverRuntimeJarToLaunch(config.getWorkingCopy()));

            if (launchdelegate != null) {
                launchdelegate.launch(
                    configurationWorkingCopy, ILaunchManager.RUN_MODE,
                    launch, SubMonitor.convert(monitor, 1));
            }

            monitor.done();
        }
    }

    protected ILaunchConfigurationWorkingCopy bindCloverRuntimeJarToLaunch(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
        return workingCopy;
    }

    protected ILaunchConfigurationWorkingCopy bindCustomOutputDirToLaunchClasspath(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
        final String projectName = workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);
        if (projectName != null) {
            final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            if (project != null) {
                final CloverProject cloverProject = CloverProject.getFor(project);
                if (cloverProject != null) {
                    final ProjectSettings settings = cloverProject.getSettings();
                    if (!settings.isOutputRootSameAsProject()) {
                        final List<String> launchClasspath = newLinkedList();
                        final Set<IPath> outputPaths = new InstrumentationProjectPathMap(cloverProject, null).getOutputPaths();
                        for (IPath outputPath : outputPaths) {
                            launchClasspath.add(
                                    JavaRuntime.newArchiveRuntimeClasspathEntry(outputPath).getMemento());
                        }

                        final List<String> origLaunchClasspath = (List<String>)workingCopy.getAttribute(
                                IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, (List)null);
                        if (origLaunchClasspath == null) {
                            final IRuntimeClasspathEntry[] classpathEntries = JavaRuntime.computeUnresolvedRuntimeClasspath(workingCopy);
                            for (IRuntimeClasspathEntry classpathEntry : classpathEntries) {
                                launchClasspath.add(classpathEntry.getMemento());
                            }
                        } else {
                            for (String cp : origLaunchClasspath) {
                                launchClasspath.add(cp);
                            }
                        }
                        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
                        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, launchClasspath);
                    }
                }
            }
        }
        return workingCopy;
    }

    @Override
    public ILaunch getLaunch(ILaunchConfiguration config, String mode) throws CoreException {
        return new Launch(config, LaunchingConstants.CLOVER_MODE, null);
    }

    @Override
    public boolean buildForLaunch(ILaunchConfiguration config, String mode, IProgressMonitor monitor) throws CoreException {
        if (launchdelegate2 == null) {
            return true;
        } else {
            return launchdelegate2.buildForLaunch(config, ILaunchManager.RUN_MODE, monitor);
        }
    }

    @Override
    public boolean finalLaunchCheck(ILaunchConfiguration config, String mode, IProgressMonitor monitor) throws CoreException {
        if (launchdelegate2 == null) {
            return true;
        } else {
            return launchdelegate2.finalLaunchCheck(config, ILaunchManager.RUN_MODE, monitor);
        }
    }

    @Override
    public boolean preLaunchCheck(ILaunchConfiguration config, String mode, IProgressMonitor monitor) throws CoreException {
        if (launchdelegate2 == null) {
            return true;
        } else {
            return launchdelegate2.preLaunchCheck(config, mode, monitor);
        }
    }

    protected String resolveCloverRuntimeJar(ILaunchConfiguration config) throws CoreException {
        return JavaRuntime.resolveRuntimeClasspathEntry(
            JavaRuntime.newVariableRuntimeClasspathEntry(CloverPlugin.CLOVER_RUNTIME_VARIABLE), config)[0].getLocation();
    }
}
