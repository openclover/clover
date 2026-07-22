package org.openclover.runtime.remote;

import org.openclover.runtime.Logger;
import org.openclover.runtime.util.IOStreamUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.openclover.runtime.util.IOStreamUtils.bufferedDataInput;
import static org.openclover.runtime.util.IOStreamUtils.bufferedDataOutput;

/**
 * Client side of distributed coverage. Connects to a {@link TcpRecorderService}, then receives slice events
 * and applies them locally via {@link MessageCodec#decodeAndDispatch}, acknowledging each so the server's
 * barrier can proceed. Reconnects on its own timer if the server is not yet up or restarts.
 *
 * @see TcpRecorderService
 */
public class TcpRecorderListener implements RecorderListener {

    private DistributedConfig config;
    private final Timer reconnectionTimer = new Timer(true);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    // Sentinel published by disconnect(): once present, no further socket may be registered. Its identity
    // is all that matters (it is never read from or written to), so an unconnected Socket is fine.
    private static final Socket DISCONNECTED = new Socket();
    // Holds the live socket, or DISCONNECTED once disconnect() has run. A CAS against this reference makes
    // the hand-off of a freshly connected socket atomic w.r.t. disconnect(), so a connect attempt that
    // completes after disconnect() cannot leak an established socket and reader thread past teardown.
    private final AtomicReference<Socket> socket = new AtomicReference<>();
    private volatile Thread readerThread;

    @Override
    public void init(final Config config) {
        this.config = (DistributedConfig) config;
    }

    @Override
    public void connect() {
        reconnect();
    }

    /**
     * Stop reconnecting and close the current connection.
     */
    @Override
    public void disconnect() {
        stopped.set(true);
        reconnectionTimer.cancel();
        // atomically claim the terminal state and close whatever socket was live (null / live / sentinel)
        IOStreamUtils.close(socket.getAndSet(DISCONNECTED));
    }

    private void reconnect() {
        if (stopped.get() || reconnecting.getAndSet(true)) {
            return;
        }
        reconnectionTimer.schedule(new ReconnectTimerTask(), 0, config.getRetryPeriod());
        Logger.getInstance().debug("Started timer to attempt reconnect every: " + config.getRetryPeriod() + " ms.");
    }

    private boolean connectOnce() {
        final String host = config.getHost();
        final int port = config.getPort();
        Socket newSocket = null;
        try {
            Logger.getInstance().debug("Attempting connection to: " + host + ":" + port);
            newSocket = new Socket();
            newSocket.connect(new InetSocketAddress(host, port));
            handshakeAndStartReader(newSocket);
            Logger.getInstance().debug("Connected to distributed coverage server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            Logger.getInstance().debug("Could not connect to server at " + host + ":" + port + ". " + e.getMessage());
            IOStreamUtils.close(newSocket);
            return false;
        }
    }

    private void handshakeAndStartReader(final Socket newSocket) throws IOException {
        final DataInputStream in = bufferedDataInput(newSocket);
        final DataOutputStream out = bufferedDataOutput(newSocket);
        MessageCodec.writeClientHandshake(out);
        MessageCodec.readServerHandshake(in);

        // Publish the connection unless disconnect() has already run. The CAS loop makes the
        // "not disconnected -> register" decision atomic against a concurrent disconnect(): if the
        // sentinel is (or becomes) present we abandon this socket instead of leaking it.
        Socket previous;
        do {
            previous = socket.get();
            if (previous == DISCONNECTED) {
                IOStreamUtils.close(newSocket);
                throw new IOException("listener disconnected during handshake");
            }
        } while (!socket.compareAndSet(previous, newSocket));

        readerThread = new Thread(() -> readerLoop(newSocket, in, out), "clover-remote-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readerLoop(final Socket connected, final DataInputStream in, final DataOutputStream out) {
        try {
            while (!stopped.get() && !connected.isClosed()) {
                applyAndAck(in, out);
            }
        } catch (EOFException e) {
            Logger.getInstance().debug("Distributed coverage server closed the connection.");
        } catch (IOException e) {
            Logger.getInstance().debug("Distributed coverage connection lost: " + e.getMessage());
        } finally {
            IOStreamUtils.close(connected);
            resumeReconnectUnlessStopped();
        }
    }

    /** Applies one slice event locally, then acknowledges it so the server's barrier can proceed. */
    private void applyAndAck(final DataInputStream in, final DataOutputStream out) throws IOException {
        MessageCodec.decodeAndDispatch(in);
        out.writeByte(MessageCodec.ACK);
        out.flush();
    }

    private void resumeReconnectUnlessStopped() {
        if (!stopped.get()) {
            // server may have restarted - resume the reconnect loop
            reconnecting.set(false);
            reconnect();
        }
    }

    private class ReconnectTimerTask extends TimerTask {
        @Override
        public void run() {
            if (stopped.get()) {
                cancel();
                return;
            }
            if (connectOnce()) {
                reconnecting.set(false);
                cancel();
            }
        }
    }
}
