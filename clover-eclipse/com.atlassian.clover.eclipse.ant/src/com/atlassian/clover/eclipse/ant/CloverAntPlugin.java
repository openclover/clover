package com.atlassian.clover.eclipse.ant;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

/**
 * Plugin to support license synchronisation between the main CloverPlugin and Ant
 * support. It is very important that this plugin does not, in any way, dynamically
 * link with the main plugin (Require-Bundle manifest entry) - as the two Clover subsystems
 * (one for Ant and the other for the GUI) make use of static logging and thus interfer
 * with each other.
 */
public class CloverAntPlugin extends AbstractUIPlugin {
    public static final String ID = "com.atlassian.clover.eclipse.ant";
    private static final String CLOVER_LICENSE_KEY = "clover_license";
    private static final String CORE_ID = "com.atlassian.clover.eclipse.core";

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
        updateLicenseFile();
    }                

    public void updateLicenseFile() throws IOException {
        final String licenseText = ConfigurationScope.INSTANCE.getNode(CORE_ID)
                .get(CLOVER_LICENSE_KEY, "");

        final File licenseFile = getLicenseFile();
        if (!licenseFile.exists()) {
            licenseFile.createNewFile();
        }
        final FileWriter writer = new FileWriter(licenseFile);
        writer.write(licenseText);
        writer.flush();
        writer.close();
    }

    public File getLicenseFile() throws IOException {
        URL locationUrl = FileLocator.find(getBundle(), new Path("/"), null);
        URL fileUrl = FileLocator.toFileURL(locationUrl);
        return new File(fileUrl.getFile(), "clover.license");
    }
}
