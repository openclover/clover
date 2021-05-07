package org.openclover.eclipse.runtime;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;

import java.io.File;
import java.net.URL;

/**
 * Provides the CLOVER_RUNTIME classpath variable which points to clover-runtime.jar
 * within the plugin.
 */
public class VariableInitialiser extends ClasspathVariableInitializer {
    private static final String RUNTIME_JAR_PATH = "/clover-runtime.jar";

    @Override
    public void initialize(String variable) {
        //Get OGSI bundle for our plugin
        Bundle bundle = CloverPlugin.getInstance().getBundle();

        if (bundle == null) {
            unsetVariable(variable);
        } else {
            URL root = bundle.getResource(RUNTIME_JAR_PATH);

            try {
                //Get a file reference through the EFS locator
                URL local = FileLocator.toFileURL(root);
                if (local != null) {
                    String path = new File(local.getPath()).getAbsolutePath();
                    JavaCore.setClasspathVariable(variable, Path.fromOSString(path), null);

                    CloverPlugin.getInstance().log(IStatus.INFO, "Setting classpath variable \"" +  variable + "\" to \"" + path + "\"", null);
                } else {
                    CloverPlugin.getInstance().log(IStatus.ERROR, "Unable to set \"" + variable + "\" classpath variable, unable to locate runtime jar", null);
                }
            } catch (Throwable e) {
                CloverPlugin.getInstance().log(IStatus.ERROR, "Unable to set \"" + variable + "\" classpath variable", e);
                unsetVariable(variable);
            }
        }
    }

    private void unsetVariable(String variable) {
        CloverPlugin.getInstance().log(IStatus.INFO, "Unsetting \"" + variable + "\" classpath variable", null);
        JavaCore.removeClasspathVariable(variable, null);
    }
}
