package com.atlassian.clover.eclipse.core.projects.builder;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Version;

public class JDTUtils {
    public static Version getJDTVersion() {
        String version = (String)Platform.getBundle("org.eclipse.jdt").getHeaders().get("Bundle-Version");
        return version == null ? Version.emptyVersion : new Version(version);
    }
}
