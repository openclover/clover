package com.atlassian.clover.idea.junit;

import com.intellij.testFramework.LightIdeaTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SockedBasedJUnitClassListProcessorIdeaTest extends LightIdeaTestCase {

    /** Socket read timeout - a value is smaller than default for faster test execution (ms) */
    private static final int READ_TIMEOUT = 100;

    /**
     * The SocketBasedJUnitClassListProcessor should wait until a boolean value is written by a server in a socket
     * channel. Test that our processor will indeed block reading the socket until timeout occurs.
     * @throws Exception
     */
    public void testDoesNotProcessFileWithoutSignal() throws Exception {
        final ServerSocket s = new ServerSocket(0);
        final int port = s.getLocalPort();

        final CountDownLatch processFileLatch = new CountDownLatch(1);
        final CountDownLatch timeoutLatch = new CountDownLatch(1);
        final SocketBasedJUnitClassListProcessor tested = new SocketBasedJUnitClassListProcessorFixture(processFileLatch);

        final Thread test = new Thread("Test timeout in SocketBasedJUnitClassListProcessor") {
            @Override
            public void run() {
                try {
                    // this method should hang as we're not writing anything to this port,
                    // so we expect that processFileLatch won't be decremented
                    tested.processWhenSocketReady(port);
                } finally {
                    // the method shall interrupt itself due to a socket read timeout
                    timeoutLatch.countDown();
                }
            }
        };

        test.start();

        // wait until socket read timeout inside SocketBasedJUnitClassListProcessor occurs
        boolean counterIsZero = timeoutLatch.await(READ_TIMEOUT * 2, TimeUnit.MILLISECONDS);

        assertTrue("Expected to have SocketBasedJUnitClassListProcessor#JUNIT_CLASS_LIST_TIMEOUT read timeout",
                counterIsZero);
        assertEquals("Expected to not process the file",
                1, processFileLatch.getCount());
        test.join();
    }

    //Wonky test - very unreliable.
//    public void testDoesProcessFileAfterSignal() throws Exception {
//        ServerSocket s = new ServerSocket(0);
//        final int port = s.getLocalPort();
//
//        // use null param so it blows up if tries to process
//
//        final CountDownLatch latch = new CountDownLatch(2);
//
//        final SocketBasedJUnitClassListProcessor tested = new SocketBasedJUnitClassListProcessorFixture(latch);
//        final AtomicInteger activePort = new AtomicInteger(Integer.MIN_VALUE);
//        Thread test = new Thread("Test timeout in SocketBasedJUnitClassListProcessor") {
//            @Override
//            public void run() {
//                activePort.set(tested.processWhenSocketReady(port));
//                latch.countDown();
//            }
//        };
//        test.start();
//        try {
//            Thread.sleep(200);
//            final Socket socket = s.accept();
//            final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
//            dataOutputStream.writeBoolean(true);
//            dataOutputStream.close();
//
//            final boolean latchTriggered = latch.await(1000, TimeUnit.MILLISECONDS);
//            assertTrue(latchTriggered);
//            assertTrue("Active port should be > 0, is " + activePort.get(), activePort.get() > 0);
//        } finally {
//            //noinspection deprecation
//            test.stop(); // yes, I know what I am doing. Also, no need to stop if latch was triggered
//        }
//    }
//
    private class SocketBasedJUnitClassListProcessorFixture extends SocketBasedJUnitClassListProcessor {
        private final CountDownLatch processLatch;

        SocketBasedJUnitClassListProcessorFixture(@NotNull CountDownLatch processLatch) {
            super(null, null, null, null, READ_TIMEOUT);
            this.processLatch = processLatch;
        }

        @Override
        boolean processFile() {
            processLatch.countDown();
            return true;
        }

        @Override
        void notifyJUnitListener(boolean wireValue) throws IOException {
        }
    }
}
