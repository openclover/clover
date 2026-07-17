package org.openclover.runtime.remote;

import org.openclover.runtime.Logger;
import org.openclover.runtime.util.Formatting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server side of distributed coverage. Accepts connections from many client JVMs and broadcasts each slice
 * event to all of them, blocking until every client has acknowledged it (the barrier that keeps clients in
 * lockstep). Uses only a raw {@link ServerSocket} and the {@link MessageCodec} wire protocol - no third-party
 * library, no object serialization.
 *
 * @see TcpRecorderListener
 */
public class TcpRecorderService implements RecorderService {

    private static final int MIN_ACCEPT_BACKLOG = 128;
    private static final int BARRIER_POLL_MILLIS = 500;

    private DistributedConfig config;
    private ServerSocket serverSocket;
    private final CopyOnWriteArrayList<ClientConnection> clients = new CopyOnWriteArrayList<>();
    private Thread acceptThread;
    private ExecutorService fanoutPool;
    private volatile boolean running;
    private final Object barrier = new Object();

    @Override
    public void init(Config config) {
        this.config = (DistributedConfig) config;
    }

    @Override
    public void start() {
        try {
            Logger.getInstance().debug("About to start service with config: " + config);
            bindServerSocket();
            running = true;
            fanoutPool = Executors.newCachedThreadPool(new DaemonThreadFactory("clover-remote-broadcast"));
            startAcceptThread();
            Logger.getInstance().debug("Started coverage service: " + config.getName());

            awaitClients();
        } catch (IOException e) {
            Logger.getInstance().error("Error starting recorder service: " + config, e);
        }
    }

    private void bindServerSocket() throws IOException {
        // size the backlog so a burst of clients connecting at once is not refused
        final int backlog = Math.max(MIN_ACCEPT_BACKLOG, config.getNumClients());
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(config.getHost(), config.getPort()), backlog);
    }

    private void startAcceptThread() {
        acceptThread = new DaemonThreadFactory("clover-remote-accept").newThread(this::acceptLoop);
        acceptThread.start();
    }

    private void awaitClients() {
        final int numClients = config.getNumClients();
        if (numClients <= 0) {
            return;
        }
        Logger.getInstance().info("OpenClover waiting for " + numClients +
                " remote clients to attach to this remote testing session. ");
        synchronized (barrier) {
            while (running && clients.size() < numClients) {
                try {
                    barrier.wait(BARRIER_POLL_MILLIS);
                    Logger.getInstance().debug("Waiting for " + numClients + " remote VMs " + clients.size());
                } catch (InterruptedException e) {
                    // ignore and re-check the condition
                }
            }
        }
        Logger.getInstance().debug("Recording proceeding now that "
                + Formatting.pluralizedVal(clients.size(), "client") + " are connected.");
    }

    private void acceptLoop() {
        while (running) {
            final Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                if (running) {
                    Logger.getInstance().debug("Accept loop stopped: " + e.getMessage());
                }
                return;
            }
            // Handshake off the accept thread and under a read timeout, so a peer that connects but never
            // sends its handshake cannot stall the accept loop (and thus the start() barrier) for everyone.
            fanoutPool.execute(() -> registerClient(socket));
        }
    }

    private void registerClient(Socket socket) {
        try {
            socket.setSoTimeout(config.getTimeout());
            final ClientConnection connection = ClientConnection.accept(socket);
            clients.add(connection);
            Logger.getInstance().debug("Accepted connection from client: " + connection);
            synchronized (barrier) {
                barrier.notifyAll();
            }
        } catch (IOException e) {
            // bad magic/version, a stalled peer, or a stray process - never dispatch, just drop it
            Logger.getInstance().info("Rejecting connection from " + socket.getRemoteSocketAddress()
                    + ": " + e.getMessage());
            closeQuietly(socket);
        }
    }

    /**
     * Encodes the event once, then broadcasts it to every client in parallel and blocks until all have
     * acknowledged (or are dropped on timeout). Latency is the slowest client, not the sum.
     *
     * @return the number of clients that successfully applied the event
     */
    @Override
    public int sendMessage(RpcMessage message) {
        final byte[] frame = encodeFrame(message);
        if (frame == null) {
            return 0;
        }

        final List<ClientConnection> snapshot = new ArrayList<>(clients);
        if (snapshot.isEmpty()) {
            return 0;
        }

        final int numSuccess = broadcastAndAwait(frame, snapshot);
        Logger.getInstance().debug("Applied " + message.getName() + " on " + numSuccess + " remote clients.");
        return numSuccess;
    }

    /** Encodes the event once, or returns {@code null} (logged) if it cannot be encoded. */
    private byte[] encodeFrame(RpcMessage message) {
        try {
            return MessageCodec.encode(message);
        } catch (IOException e) {
            Logger.getInstance().error("Could not encode remote coverage message: " + e.getMessage(), e);
            return null;
        }
    }

    /** Fans the frame out to every client concurrently and blocks until all have ACKed or been dropped. */
    private int broadcastAndAwait(byte[] frame, List<ClientConnection> recipients) {
        final int timeout = config.getTimeout();
        final CountDownLatch latch = new CountDownLatch(recipients.size());
        final AtomicInteger successes = new AtomicInteger(0);
        for (final ClientConnection connection : recipients) {
            fanoutPool.execute(() -> sendToClient(connection, frame, timeout, successes, latch));
        }
        awaitAcks(latch, timeout);
        return successes.get();
    }

    /** Sends the frame to one client and counts its ACK, dropping the client on any failure. */
    private void sendToClient(ClientConnection connection, byte[] frame, int timeout,
                              AtomicInteger successes, CountDownLatch latch) {
        try {
            connection.sendAndAwaitAck(frame, timeout);
            successes.incrementAndGet();
        } catch (Exception e) {
            Logger.getInstance().warn("Error during remote flush to " + connection
                    + ": " + e.getMessage() + " - dropping client", e);
            clients.remove(connection);
            connection.closeQuietly();
        } finally {
            latch.countDown();
        }
    }

    private void awaitAcks(CountDownLatch latch, int timeout) {
        try {
            // each task self-terminates within `timeout` via the socket read timeout; the extra margin
            // just guards against scheduling latency before we proceed (fail-soft).
            if (!latch.await((long) timeout + BARRIER_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                Logger.getInstance().warn("Timed out waiting for remote clients to acknowledge coverage flush");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        running = false;
        closeQuietly(serverSocket);
        synchronized (barrier) {
            barrier.notifyAll();
        }
        for (ClientConnection connection : clients) {
            connection.closeQuietly();
        }
        clients.clear();
        if (fanoutPool != null) {
            fanoutPool.shutdownNow();
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
    }

    public int getNumRegisteredListeners() {
        return clients.size();
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Logger.getInstance().debug("Error closing " + closeable + ": " + e.getMessage());
            }
        }
    }
}
