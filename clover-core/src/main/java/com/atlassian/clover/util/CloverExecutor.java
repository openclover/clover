package com.atlassian.clover.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;


/**
 * An interface exposing only methods used by Clover from the ExecutorService interface of util.concurrent.
 */
public interface CloverExecutor {


    /**
     * @see java.util.concurrent.ExecutorService#shutdown()
     */
    void shutdown();

    /**
     * @see java.util.concurrent.ExecutorService#awaitTermination(long,
     *      java.util.concurrent.TimeUnit)
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * @see java.util.concurrent.ExecutorService#submit(Runnable) 
     */
    void  submit(Callable task) throws Exception;

}
