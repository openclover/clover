package org.openclover.runtime.remote;

import org.openclover.runtime.util.IOStreamUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Server-side handle for a single connected client. Because slice boundaries are serialized there is
 * at most one in-flight event per client, so no outbound queue is needed and a single read/write pair suffices.
 *
 * @see TcpRecorderService
 */
class ClientConnection {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    ClientConnection(final Socket socket, final DataInputStream in, final DataOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    /**
     * Accepts a client socket: reads and validates the client handshake, replies with the server handshake,
     * and returns a ready {@link ClientConnection}.
     */
    static ClientConnection accept(final Socket socket) throws IOException {
        final DataInputStream in = IOStreamUtils.bufferedDataInput(socket);
        final DataOutputStream out = IOStreamUtils.bufferedDataOutput(socket);
        MessageCodec.readClientHandshake(in);
        MessageCodec.writeServerHandshake(out);
        return new ClientConnection(socket, in, out);
    }

    /**
     * Writes one encoded slice event, flushes it, and blocks until the client acknowledges it has applied
     * the event (the per-client half of the barrier). A missing/late/wrong ACK or any I/O error throws so the
     * caller can drop this connection.
     *
     * @param frame   the encoded slice event
     * @param msTimeout maximum time to wait for the ACK, in milliseconds
     */
    void sendAndAwaitAck(final byte[] frame, final int msTimeout) throws IOException {
        socket.setSoTimeout(msTimeout);
        out.write(frame);
        out.flush();
        final byte ack = in.readByte();
        if (ack != MessageCodec.ACK) {
            throw new IOException("Unexpected acknowledgement byte from client "
                    + socket.getRemoteSocketAddress() + ": " + ack);
        }
    }

    void closeQuietly() {
        IOStreamUtils.close(socket);
    }

    @Override
    public String toString() {
        return "ClientConnection{remote=" + socket.getRemoteSocketAddress() + "}";
    }
}
