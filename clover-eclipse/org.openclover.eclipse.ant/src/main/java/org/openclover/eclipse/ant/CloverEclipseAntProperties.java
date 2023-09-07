package org.openclover.eclipse.ant;

import org.eclipse.ant.core.IAntPropertyValueProvider;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;

public class CloverEclipseAntProperties implements IAntPropertyValueProvider {

    @Override
    public String getAntPropertyValue(String propertyName) {
        if ("clover.eclipse.runtime.jar".equals(propertyName)) {
            Bundle runtimeBundle = Platform.getBundle("org.openclover.eclipse.runtime");
            URL jarUrl = runtimeBundle == null
                    ? null
                    : FileLocator.find(runtimeBundle, new Path("/clover-runtime.jar"), null);

            jarUrl = jarUrl == null
                    ? FileLocator.find(CloverAntPlugin.getInstance().getBundle(), new Path("/clover-ant-eclipse.jar"), null)
                    : jarUrl;
            try {
                return jarUrl == null ? null : FileLocator.toFileURL(jarUrl).getFile();
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
