package org.openclover.core.remote

import org.junit.After
import org.junit.Test
import org.openclover.runtime.Logger
import org.openclover.runtime.remote.DistributedConfig
import org.openclover.runtime.remote.MessageCodec
import org.openclover.runtime.remote.RemoteFactory
import org.openclover.runtime.remote.RpcMessage
import org.openclover.runtime.remote.TcpRecorderService
import org.openclover.runtime.util.IOStreamUtils

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Test for {@link TcpRecorderService}. Uses raw protocol-speaking sockets as clients (rather than a real
 * {@link org.openclover.runtime.remote.TcpRecorderListener}) so the broadcast, barrier and ACK behaviour can
 * be observed deterministically without initialising the {@link org_openclover_runtime.Clover} runtime.
 */
class TcpRecorderServiceTest {

    private static final String KEY_TCP_PORT = "clover.tcp.port"
    static final String TEST_PORT = System.getProperty(KEY_TCP_PORT, "1199")

    private TcpRecorderService service
    private final List<Socket> clientSockets = new ArrayList<>()

    @After
    void tearDown() {
        clientSockets.each { closeQuietly(it) }
        service?.stop()
    }

    private DistributedConfig config(int timeout, int numClients) {
        (DistributedConfig) RemoteFactory.getInstance().createConfig(
                "${DistributedConfig.PORT}=${TEST_PORT};${DistributedConfig.TIMEOUT}=${timeout};${DistributedConfig.NUM_CLIENTS}=${numClients}")
    }

    /** Opens a raw socket, performs the client handshake and returns it registered in {@link #clientSockets}. */
    private RawClient connectRawClient() {
        final Socket socket = new Socket()
        socket.connect(new InetSocketAddress("localhost", Integer.parseInt(TEST_PORT)), 2000)
        clientSockets.add(socket)
        final DataInputStream input = IOStreamUtils.bufferedDataInput(socket)
        final DataOutputStream output = IOStreamUtils.bufferedDataOutput(socket)
        MessageCodec.writeClientHandshake(output)
        MessageCodec.readServerHandshake(input)
        new RawClient(input, output)
    }

    private static void waitForRegistered(TcpRecorderService service, int expected) {
        final long deadline = System.currentTimeMillis() + 5000
        while (service.getNumRegisteredListeners() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(expected, service.getNumRegisteredListeners())
    }

    @Test
    void testBroadcastAndAck() {
        service = (TcpRecorderService) RemoteFactory.getInstance().createService(config(2000, 0))
        service.start()

        final RawClient a = connectRawClient()
        final RawClient b = connectRawClient()
        waitForRegistered(service, 2)

        final RpcMessage msg = RpcMessage.createMethodStart("test.Type", 5, 987654321L)

        final def pool = Executors.newSingleThreadExecutor()
        final Future<Object> sent = pool.submit({ service.sendMessage(msg) } as java.util.concurrent.Callable)

        // both clients receive the same ordered START frame, then ACK
        [a, b].each { it.readAndAckSliceStart("test.Type", 5, 987654321L) }

        assertEquals(2, sent.get(5, TimeUnit.SECONDS))
        pool.shutdownNow()
    }

    @Test
    void testSlowClientIsDroppedOthersUnaffected() {
        service = (TcpRecorderService) RemoteFactory.getInstance().createService(config(500, 0))
        service.start()

        final RawClient good = connectRawClient()
        connectRawClient() // second slow client, never ACKs
        waitForRegistered(service, 2)

        final def pool = Executors.newSingleThreadExecutor()
        final Future<Object> sent = pool.submit(
                { service.sendMessage(RpcMessage.createMethodStart("t", 1, 1L)) } as java.util.concurrent.Callable)

        good.readAndAckSliceStart("t", 1, 1L)
        // the slow client is dropped on timeout; only the good client is counted and remains registered
        assertEquals(1, sent.get(5, TimeUnit.SECONDS))
        waitForRegistered(service, 1)
        pool.shutdownNow()
    }

    @Test
    void testStalledClientDoesNotBlockAcceptOfOthers() {
        // short handshake timeout so the stalled peer is dropped quickly
        service = (TcpRecorderService) RemoteFactory.getInstance().createService(config(500, 0))
        service.start()

        // connects but never sends the handshake bytes - must not wedge the accept loop
        final Socket stalled = new Socket()
        stalled.connect(new InetSocketAddress("localhost", Integer.parseInt(TEST_PORT)), 2000)
        clientSockets.add(stalled)

        // a well-behaved client that connects afterwards still registers
        connectRawClient()
        waitForRegistered(service, 1)
    }

    @Test
    void testStartBlocksUntilNumClientsConnect() {
        service = (TcpRecorderService) RemoteFactory.getInstance().createService(config(2000, 1))

        final def pool = Executors.newSingleThreadExecutor()
        final Future<?> started = pool.submit({ service.start() } as Runnable)

        // start() must not complete until a client attaches
        Thread.sleep(500)
        assertTrue("start() should still be blocking on the barrier", !started.isDone())

        connectRawClient() // late client
        started.get(5, TimeUnit.SECONDS)    // the barrier releases once the client is registered
        assertEquals(1, service.getNumRegisteredListeners())
        pool.shutdownNow()
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close()
        } catch (IOException ignored) {
        }
    }

    /** A raw protocol client used by the tests in place of the real listener. */
    private static class RawClient {
        final DataInputStream input
        final DataOutputStream output

        RawClient(DataInputStream input, DataOutputStream output) {
            this.input = input
            this.output = output
        }

        void readAndAckSliceStart(String expectedType, int expectedSlice, long expectedStart) {
            assertEquals(MessageCodec.OP_SLICE_START, input.readByte())
            assertEquals(expectedType, input.readUTF())
            assertEquals(expectedSlice, input.readInt())
            assertEquals(expectedStart, input.readLong())
            output.writeByte(MessageCodec.ACK)
            output.flush()
        }
    }
}
