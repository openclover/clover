package org.openclover.eclipse.core.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.openclover.eclipse.core.CloverPlugin;

public class CloverTabGroup implements ILaunchConfigurationTabGroup, IExecutableExtension {
    private static final String LAUNCH_TABGROUPS_EXTENSION = "org.eclipse.debug.ui.launchConfigurationTabGroups";

    private ILaunchConfigurationTabGroup delegate;

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        delegate = delegateTabGroupFor(config.getAttribute(LaunchingConstants.TYPE_CONFIG_ATTRIBUTE));
    }

    protected ILaunchConfigurationTabGroup delegateTabGroupFor(String launchType) throws CoreException {
        IConfigurationElement element = findRunModeConfigurationFor(launchType);
        if (element == null) {
            throw CloverPlugin.logAndThrowError("There are no tab groups registered to run for launch type of " + launchType);
        } else {
            return (ILaunchConfigurationTabGroup) element.createExecutableExtension(LaunchingConstants.CLASS_EXTENSION);
        }
    }

    /**
     * @param type of the launch, e.g. "org.eclipse.jdt.launching.localJavaApplication",
     * "org.eclipse.pde.ui.JunitLaunchConfig"
     * @return configuration for the "run" mode or null if one not found
     */
    private IConfigurationElement findRunModeConfigurationFor(String type) {
        IConfigurationElement[] tabGroupConfigs =
            Platform.getExtensionRegistry().getExtensionPoint(
                LAUNCH_TABGROUPS_EXTENSION).getConfigurationElements();

        //Finds //launchConfigurationTabGroup extensions where @type == type and ./launchMode@ = "run"
        //thus find all config for tab groups that will operate in mode "run" when we need them to operate
        //in mode "clover"
        IConfigurationElement result = null;
        for (IConfigurationElement tabGroupConfig : tabGroupConfigs) {
            if (type.equals(tabGroupConfig.getAttribute(LaunchingConstants.TYPE_CONFIG_ATTRIBUTE))) {
                IConfigurationElement[] launchModeConfigs = tabGroupConfig.getChildren(LaunchingConstants.LAUNCH_MODE_CONFIG_NAME);
                if (launchModeConfigs.length > 0) {
                    for (IConfigurationElement launchModeConfig : launchModeConfigs) {
                        if (ILaunchManager.RUN_MODE.equals(launchModeConfig.getAttribute(LaunchingConstants.MODE_ATTRIBUTE))) {
                            return tabGroupConfig;
                        }
                    }
                } else {
                    //If there are no user launch mode configurations yet, default to this
                    //until we find a better one later
                    result = tabGroupConfig;
                }
            }
        }
        return result;
    }

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        delegate.createTabs(dialog, mode);
    }

    @Override
    public ILaunchConfigurationTab[] getTabs() {
        return delegate.getTabs();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        delegate.setDefaults(configuration);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        delegate.initializeFrom(configuration);
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        delegate.performApply(configuration);
    }

    /**
     * @deprecated
     */
    @Override
    public void launched(ILaunch launch) { /* NO IMPL */ }
}