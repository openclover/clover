package org.openclover.eclipse.core;

import org.eclipse.core.runtime.jobs.Job;

public abstract class SystemJob extends Job {

    public SystemJob(String name) {
        super(name);
        setSystem(true);
        setUser(false);
    }
}
