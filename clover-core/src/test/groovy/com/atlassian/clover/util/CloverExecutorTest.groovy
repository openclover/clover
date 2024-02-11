package com.atlassian.clover.util

import org.junit.Test

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import org.openclover.runtime.Logger

import static org.junit.Assert.*

public class CloverExecutorTest {

    private static class TestCallable implements Callable {
        boolean isCalled = false

        public Object call() throws Exception {
            Thread.sleep(1000)
            isCalled = true
            return null
        }
    }

    final RuntimeException runtimeException = new RuntimeException("Testing logging.")

    private class ExceptionCallable implements Callable {
        public Object call() throws Exception {
            throw runtimeException
        }
    }

    private final TestCallable task1 = new TestCallable()
    private final TestCallable task2 = new TestCallable()

    @Test
    public void testThreadedExecutor() throws Exception {
        CloverExecutor executor = CloverExecutors.newCloverExecutor(2, "CLOVER-TEST")
        submitTasks(executor, false)
    }

    @Test
    public void testSynchronousExecutor() throws Exception {
        CloverExecutor executor = CloverExecutors.newCloverExecutor(0, "CLOVER-TEST")
        submitTasks(executor, true)
    }

    @Test
    public void testExceptionHandling() {
        CloverExecutor executor = CloverExecutors.newCloverExecutor(10, "CLOVER-EXCEPTION-TEST")
        Logger logger = Logger.getInstance()
        try {
            RecordingLogger bufferLogger = new RecordingLogger()
            Logger.setInstance(bufferLogger)
            executor.submit(new ExceptionCallable())
            executor.awaitTermination(1000, TimeUnit.MILLISECONDS)
            assertTrue(bufferLogger.contains(runtimeException))
            assertTrue(bufferLogger.contains(runtimeException.getMessage()))
        } catch (Exception e) {
            fail("Exception thrown, which should have been caught and logged." + e)
        } finally {
            Logger.setInstance(logger)
        }
    }
    

    private void submitTasks(CloverExecutor executor, boolean expectSynchronous) throws Exception {
        executor.submit(task1)
        assertEquals(expectSynchronous, task1.isCalled)
        executor.submit(task2)
        assertEquals(expectSynchronous, task2.isCalled);        
        executor.shutdown()
        assertTrue(executor.awaitTermination(2000, TimeUnit.MILLISECONDS))
        assertTrue(task1.isCalled)
        assertTrue(task2.isCalled)
    }

}
