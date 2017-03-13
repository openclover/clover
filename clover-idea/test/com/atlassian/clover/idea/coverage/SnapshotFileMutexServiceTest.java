package com.atlassian.clover.idea.coverage;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SnapshotFileMutexServiceTest extends TestCase {
    private SnapshotFileMutex snapshotFileMutexService;
    private CountDownLatch entryLatch;
    private CountDownLatch exitLatch;
    private CyclicBarrier middleBarrier;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        snapshotFileMutexService = new SnapshotFileMutexService();
        entryLatch = new CountDownLatch(1);
        exitLatch = new CountDownLatch(1);
        middleBarrier = new CyclicBarrier(2);
    }

    public void testLockingTheSameFile() throws InterruptedException, BrokenBarrierException {
        final File snapshot = new File(FileUtil.getTempDirectory(), "snapshot");

        snapshotFileMutexService.lockFile(snapshot);

        MyThread otherThread = new MyThread(snapshot);
        otherThread.start();

        assertTrue("Waiting for the other thread", entryLatch.await(1, TimeUnit.SECONDS));
        assertFalse("Other should not have continued", otherThread.passed.get());

        try {
            middleBarrier.await(100, TimeUnit.MILLISECONDS);
            fail("Other thread should be stalled");
        } catch (TimeoutException e) {
            // expected - other thread should be stalled
        }

        snapshotFileMutexService.releaseFile(snapshot);

        assertTrue("Waiting for the other thread", exitLatch.await(1, TimeUnit.SECONDS));
        assertTrue("Other should have continued", otherThread.passed.get());
    }

    public void testLockingDifferentFile() throws InterruptedException, BrokenBarrierException, TimeoutException {
        File snapshot1 = new File(FileUtil.getTempDirectory(), "snapshot1");
        File snapshot2 = new File(FileUtil.getTempDirectory(), "snapshot2");

        snapshotFileMutexService.lockFile(snapshot1);

        MyThread otherThread = new MyThread(snapshot2);
        otherThread.start();

        assertTrue("Waiting for the other thread", entryLatch.await(1, TimeUnit.SECONDS));
        middleBarrier.await(100, TimeUnit.MILLISECONDS);

        assertTrue("Other should have continued", otherThread.passed.get());

        snapshotFileMutexService.releaseFile(snapshot1);

        assertTrue("Waiting for the other thread", exitLatch.await(1, TimeUnit.SECONDS));
    }


    public void testMemoryLeak() {
        final File snapshot = new File(FileUtil.getTempDirectory(), "snapshot");
        snapshotFileMutexService.lockFile(snapshot);
        snapshotFileMutexService.releaseFile(snapshot);

        assertEquals(0, ((SnapshotFileMutexService)snapshotFileMutexService).mapSize());

    }

    private class MyThread extends Thread {
        private final File file;
        final AtomicBoolean passed = new AtomicBoolean();
        Exception barrierException;

        public MyThread(File file) {
            this.file = file;
        }

        @Override
            public void run() {
            entryLatch.countDown();
            snapshotFileMutexService.lockFile(file);
            passed.set(true);
            try {
                middleBarrier.await(100, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                barrierException = e;
            }
            snapshotFileMutexService.releaseFile(file);
            exitLatch.countDown();
        }
    }
}
