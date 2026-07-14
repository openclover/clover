package org.openclover.idea.junit;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Focused tests for {@link SocketBasedJUnitClassListProcessor#connectToJUnitSyncSocket(int)}, the retrying connect
 * added. Since IDEA 2024+ binds its JUnit synchronization socket lazily - in
 * JavaTestFrameworkRunnableState.resolveServerSocketPort(), inside createHandler(), i.e. <em>after</em> the program
 * runner has patched the run parameters - an eager connect fails with 'Connection refused'. The processor must
 * instead poll until IDEA binds the socket, and give up if it never does.
 * These use plain loopback sockets and drive the connect method directly, so they need no IDEA test fixture.
 */
public class SocketBasedJUnitClassListProcessorConnectTest extends TestCase {

    /** How long a test waits for the worker before declaring it stuck. */
    private static final Duration TEST_DEADLINE = Duration.ofSeconds(5);

    /** Delay before the fake IDEA server socket is bound / the configured give-up timeout. */
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(150);

    private static SocketBasedJUnitClassListProcessor newProcessor() {
        return new SocketBasedJUnitClassListProcessor(null, null, null, null);
    }

    private static int reserveFreePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /**
     * The socket is bound only after a delay - the processor must have been retrying with 'Connection refused'
     * until then, and must connect once it becomes available.
     */
    public void testConnectsWhenServerSocketBoundLate() throws Exception {
        final int port = reserveFreePort();
        final SocketBasedJUnitClassListProcessor tested = newProcessor();

        final AtomicBoolean connected = new AtomicBoolean(false);
        final CountDownLatch done = new CountDownLatch(1);
        final Thread worker = new Thread("connectToJUnitSyncSocket - late bind") {
            @Override
            public void run() {
                try {
                    connected.set(tested.connectToJUnitSyncSocket(port));
                } finally {
                    done.countDown();
                }
            }
        };
        worker.start();

        Thread.sleep(SHORT_TIMEOUT.toMillis());
        try (ServerSocket ideaSocket = new ServerSocket(port)) {
            assertNotNull(ideaSocket); // keep the socket bound until the worker connects
            assertTrue("Processor should connect within " + TEST_DEADLINE.toMillis() + " ms once the socket is bound",
                    done.await(TEST_DEADLINE.toMillis(), TimeUnit.MILLISECONDS));
        } finally {
            tested.close();
        }
        worker.join();
        assertTrue("connectToJUnitSyncSocket should return true once the socket is bound", connected.get());
    }

    /**
     * Nothing ever binds the port (e.g. target-environment preparation failed) - the processor must give up
     * after the connect timeout rather than spinning forever.
     */
    public void testGivesUpWhenServerSocketNeverBound() throws Exception {
        final int port = reserveFreePort(); // nothing will ever listen here
        final SocketBasedJUnitClassListProcessor tested = newProcessor();
        // give up quickly instead of the 5s production default
        tested.setSocketConnectTimeouts(SHORT_TIMEOUT, Duration.ofMillis(10));

        final long start = System.currentTimeMillis();
        final boolean connected = tested.connectToJUnitSyncSocket(port);
        final long elapsed = System.currentTimeMillis() - start;
        tested.close();

        assertFalse("Should give up when nothing ever binds the socket", connected);
        assertTrue("Should give up around the configured timeout, took " + elapsed + " ms",
                elapsed < TEST_DEADLINE.toMillis());
    }
}
