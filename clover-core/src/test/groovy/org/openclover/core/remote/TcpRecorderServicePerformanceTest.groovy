package org.openclover.core.remote

import org.junit.After
import org.junit.Test
import org.openclover.runtime.remote.DistributedConfig
import org.openclover.runtime.remote.MessageCodec
import org.openclover.runtime.remote.RemoteFactory
import org.openclover.runtime.remote.RpcMessage
import org.openclover.runtime.remote.TcpRecorderService

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Performance / scalability tests for {@link TcpRecorderService}: many clients, many broadcasts, and clients
 * of differing speeds. Clients are raw protocol sockets (not the real listener) so no {@code Clover} runtime
 * is initialised. Sizes are overridable via system properties for CI tuning.
 */
class TcpRecorderServicePerformanceTest {

    private static final String PORT = System.getProperty("clover.perf.tcp.port", "1200")
    private static final int NUM_CLIENTS = Integer.getInteger("clover.perf.clients", 500)
    private static final int NUM_MESSAGES = Integer.getInteger("clover.perf.messages", 100)
    private static final int GENEROUS_TIMEOUT_MS = 10000

    private TcpRecorderService service
    private final List<PerfClient> clients = new CopyOnWriteArrayList<>()

    @After
    void tearDown() {
        clients.each { it.close() }
        service?.stop()
    }

    private void startService() {
        final DistributedConfig config = (DistributedConfig) RemoteFactory.getInstance().createConfig(
                "${DistributedConfig.PORT}=${PORT};${DistributedConfig.TIMEOUT}=${GENEROUS_TIMEOUT_MS}")
        service = (TcpRecorderService) RemoteFactory.getInstance().createService(config)
        service.start()
    }

    private void connectClients(int count, long ackDelayMillis) {
        count.times {
            final PerfClient client = new PerfClient(Integer.parseInt(PORT), ackDelayMillis)
            client.start()
            clients.add(client)
        }
    }

    private void awaitAllRegistered(int expected) {
        final long deadline = System.currentTimeMillis() + 30000
        while (service.getNumRegisteredListeners() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        assertEquals(expected, service.getNumRegisteredListeners())
    }

    /**
     * Many clients, many messages: broadcast {@link #NUM_MESSAGES} START+END pairs to {@link #NUM_CLIENTS}
     * clients and report throughput. Every broadcast must be applied by every client (a full barrier).
     */
    @Test
    void testManyClientsManyMessages() {
        startService()
        connectClients(NUM_CLIENTS, 0L)
        awaitAllRegistered(NUM_CLIENTS)

        final long start = System.nanoTime()
        for (int i = 0; i < NUM_MESSAGES; i++) {
            assertEquals(NUM_CLIENTS, service.sendMessage(RpcMessage.createMethodStart("test.Type", i, 1L)))
            assertEquals(NUM_CLIENTS, service.sendMessage(RpcMessage.createMethodEnd("test.Type", "m", "n", i, 0, null)))
        }
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000L

        final int broadcasts = NUM_MESSAGES * 2
        final double perBroadcast = elapsedMs / (double) broadcasts
        println "[perf] ${NUM_CLIENTS} clients x ${broadcasts} broadcasts: ${elapsedMs} ms total, " +
                "${String.format('%.3f', perBroadcast)} ms/broadcast, " +
                "${(broadcasts * NUM_CLIENTS * 1000L).intdiv(Math.max(1L, elapsedMs))} client-acks/sec"

        // loose upper bound: this is a hang guard, not a benchmark gate - the printed numbers are the signal
        assertTrue("Broadcasting took too long: ${elapsedMs} ms", elapsedMs < 10000)
    }

    /**
     * With a parallel fan-out barrier the cost of a broadcast is the <b>slowest</b> client, not the sum of
     * all clients. Half the clients ACK slowly; a single broadcast must still finish far under the serial sum.
     */
    @Test
    void testLatencyBoundedBySlowestClientNotSum() {
        final int fast = 20
        final int slow = 20
        final long slowAckMillis = 50

        startService()
        connectClients(fast, 0L)
        connectClients(slow, slowAckMillis)
        awaitAllRegistered(fast + slow)

        // warm up so the broadcast thread pool is already populated when we measure
        service.sendMessage(RpcMessage.createMethodStart("t", 0, 1L))

        final long start = System.nanoTime()
        final int applied = service.sendMessage(RpcMessage.createMethodStart("t", 1, 1L))
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000L

        assertEquals(fast + slow, applied)

        final long serialSum = slow * slowAckMillis   // = 1000 ms if broadcasts were serial
        println "[perf] mixed-speed broadcast to ${fast + slow} clients: ${elapsedMs} ms " +
                "(serial sum would be ~${serialSum} ms, parallel max ~${slowAckMillis} ms)"
        assertTrue("Broadcast latency ${elapsedMs} ms looks serial, not parallel (slowest-client) bounded",
                elapsedMs < serialSum / 2)
    }

    /** A raw protocol client whose reader thread consumes each frame, optionally pauses, then ACKs. */
    private static class PerfClient {
        private final Socket socket
        private final DataInputStream input
        private final DataOutputStream output
        private final long ackDelayMillis
        private volatile boolean running = true
        final AtomicInteger received = new AtomicInteger(0)
        private Thread reader

        PerfClient(int port, long ackDelayMillis) {
            this.ackDelayMillis = ackDelayMillis
            socket = new Socket()
            socket.connect(new InetSocketAddress("localhost", port), 5000)
            input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))
            output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))
            MessageCodec.writeClientHandshake(output)
            MessageCodec.readServerHandshake(input)
        }

        void start() {
            reader = new Thread({ readLoop() })
            reader.setDaemon(true)
            reader.start()
        }

        private void readLoop() {
            try {
                while (running) {
                    consumeFrame(input)
                    if (ackDelayMillis > 0) {
                        Thread.sleep(ackDelayMillis)
                    }
                    output.writeByte(MessageCodec.ACK)
                    output.flush()
                    received.incrementAndGet()
                }
            } catch (Exception ignored) {
                // socket closed at teardown or connection lost - stop reading
            }
        }

        void close() {
            running = false
            try {
                socket.close()
            } catch (IOException ignored) {
            }
        }
    }

    /** Reads and discards exactly one slice-event frame, mirroring the {@link MessageCodec} wire format. */
    private static void consumeFrame(DataInputStream stream) {
        final byte opcode = stream.readByte()
        if (opcode == MessageCodec.OP_SLICE_START) {
            stream.readUTF(); stream.readInt(); stream.readLong()
        } else if (opcode == MessageCodec.OP_SLICE_END) {
            stream.readUTF()
            skipNullableUtf(stream)     // method
            skipNullableUtf(stream)     // runtimeTestName
            stream.readInt(); stream.readInt()
            if (stream.readBoolean()) { // ErrorInfo present
                skipNullableUtf(stream)
                skipNullableUtf(stream)
            }
        } else {
            throw new IOException("Unexpected opcode: " + opcode)
        }
    }

    private static void skipNullableUtf(DataInputStream stream) {
        if (stream.readBoolean()) {
            stream.readUTF()
        }
    }
}
