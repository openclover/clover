package org.openclover.runtime.remote;

import org.openclover.runtime.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private volatile Socket socket;
    private volatile Thread readerThread;

    @Override
    public void init(Config config) {
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
        closeSocket();
    }

    /**
     * Retained only to satisfy {@link RecorderListener}; slice events are applied directly by the reader
     * loop, not through this method. Dead once the interface can drop it.
     */
    @Override
    public Object handleMessage(RpcMessage message) {
        return "SUCCESS";
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
            closeQuietly(newSocket);
            return false;
        }
    }

    private void handshakeAndStartReader(Socket newSocket) throws IOException {
        final DataInputStream in = new DataInputStream(new BufferedInputStream(newSocket.getInputStream()));
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(newSocket.getOutputStream()));
        MessageCodec.writeClientHandshake(out, config.getName());
        MessageCodec.readServerHandshake(in);

        this.socket = newSocket;
        readerThread = new Thread(() -> readerLoop(newSocket, in, out), "clover-remote-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readerLoop(Socket connected, DataInputStream in, DataOutputStream out) {
        try {
            while (!stopped.get() && !connected.isClosed()) {
                applyAndAck(in, out);
            }
        } catch (EOFException e) {
            Logger.getInstance().debug("Distributed coverage server closed the connection.");
        } catch (IOException e) {
            Logger.getInstance().debug("Distributed coverage connection lost: " + e.getMessage());
        } finally {
            closeQuietly(connected);
            resumeReconnectUnlessStopped();
        }
    }

    /** Applies one slice event locally, then acknowledges it so the server's barrier can proceed. */
    private void applyAndAck(DataInputStream in, DataOutputStream out) throws IOException {
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

    private void closeSocket() {
        final Socket current = socket;
        if (current != null) {
            closeQuietly(current);
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Logger.getInstance().debug("Error closing socket: " + e.getMessage());
            }
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
