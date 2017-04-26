package com.atlassian.clover.eclipse.runtime;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

public class CloverPlugin extends Plugin {
    public static final String ID = "org.openclover.eclipse.runtime";

    public static CloverPlugin instance;

    public static CloverPlugin getInstance() {
        return instance;
    }

    public CloverPlugin() {
        super();
        instance = this;
    }

    public static void log(int level, String message, Throwable t) {
        getInstance().getLog().log(new Status(level, ID, 0, message, t));
    }
}
