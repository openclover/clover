package org.openclover.runtime.remote;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} that produces daemon threads with a readable, numbered name so the distributed
 * coverage transport never keeps the JVM alive.
 */
class DaemonThreadFactory implements ThreadFactory {

    private final String namePrefix;
    private final AtomicInteger counter = new AtomicInteger(0);

    DaemonThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread thread = new Thread(r, namePrefix + "-" + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }
}
