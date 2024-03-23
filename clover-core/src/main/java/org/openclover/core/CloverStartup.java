package org.openclover.core;

import org.openclover.core.util.ClassPathUtil;
import org.openclover.runtime.Logger;
import org_openclover_runtime.CloverVersionInfo;

public class CloverStartup {

    public static void logVersionInfo(Logger log) {
        log.info("OpenClover Version " + CloverVersionInfo.RELEASE_NUM +
                ", built on " + CloverVersionInfo.BUILD_DATE);
        String loadedFrom =  ClassPathUtil.getCloverJarPath();
        if (loadedFrom != null) {
            log.debug("Loaded from: " + loadedFrom);
        } else {
            log.debug("Couldn't determine path we were loaded from.");
        }
    }

}