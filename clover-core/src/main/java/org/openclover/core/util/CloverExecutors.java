package org.openclover.core.util;

import org.openclover.runtime.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Factory methods for creating Clover specific ExecutorServices.
 */
public class CloverExecutors {


    private CloverExecutors() { }

    /**
     * Creates either a fixed thread pool, if numThreads &gt; 0,
     * or a non-threaded executor service if numThreads &lt;= 0.
     * 
     * @param numThreads must be &gt;= 0
     * @param threadPrefix should not be <code>null</code>
     * @return a {@link CloverExecutorService}, that supports both threaded (if numThreads > 0) and non-threaded
     * (numThreads == 0) execution of tasks
     */
    public static CloverExecutor newCloverExecutor(int numThreads, final String threadPrefix) {
        return new CloverExecutorService(numThreads, threadPrefix);
    }

    private static class CloverExecutorService implements CloverExecutor {

        private final ExecutorService service;

        public CloverExecutorService(int numThreads, final String threadPrefix) {
            final CloverExceptionHandler handler = new CloverExceptionHandler();
            if (numThreads > 0) {
                this.service = Executors.newFixedThreadPool(numThreads, r -> {
                    Thread thread = new Thread(r);
                    thread.setUncaughtExceptionHandler(handler);
                    thread.setName(threadPrefix + "-" + thread.getName());
                    return thread;
                });
            } else {
                service = null;
            }
        }

        @Override
        public void shutdown() {
            if (service != null) {
                service.shutdown();
            }
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return service == null || service.awaitTermination(timeout, unit);
        }

        @Override
        public void submit(Callable task) throws Exception {

            if (service != null) {
                service.submit(new LoggingCallable(task));
            } else {
                task.call();
            }
        }
    }

    /**
     * A Callable which will log any exceptions thrown during execution.
     */
    private static class LoggingCallable implements Callable {
        private final Callable task;
        public LoggingCallable(Callable callable) {
            task = callable;
        }

        @Override
        public Object call() throws Exception {
            try {
                 return task.call();
            } catch (Throwable e) {
                Logger.getInstance().warn(e);
            }
            return null;
        }

    }

    /**
     * A Handler to log any uncaught exceptions thrown from the thread queues.
     */
    private static class CloverExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            Logger.getInstance().error(thread.getName(), throwable);
        }
    }
}
