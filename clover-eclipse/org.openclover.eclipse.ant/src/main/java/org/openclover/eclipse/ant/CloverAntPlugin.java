package org.openclover.eclipse.ant;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Plugin to support license synchronisation between the main CloverPlugin and Ant
 * support. It is very important that this plugin does not, in any way, dynamically
 * link with the main plugin (Require-Bundle manifest entry) - as the two Clover subsystems
 * (one for Ant and the other for the GUI) make use of static logging and thus interfer
 * with each other.
 */
public class CloverAntPlugin extends AbstractUIPlugin {
    public static final String ID = "org.openclover.eclipse.ant";

    private static CloverAntPlugin INSTANCE;

    public static CloverAntPlugin getInstance() {
        return INSTANCE;
    }

    public CloverAntPlugin() {
        INSTANCE = this;
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
    }

}
