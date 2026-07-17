package org.openclover.runtime.remote;

import org.openclover.runtime.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
    final String name;

    ClientConnection(Socket socket, DataInputStream in, DataOutputStream out, String name) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.name = name;
    }

    /**
     * Accepts a client socket: reads and validates the client handshake, replies with the server handshake,
     * and returns a ready {@link ClientConnection}.
     */
    static ClientConnection accept(Socket socket) throws IOException {
        final DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        final String name = MessageCodec.readClientHandshake(in);
        MessageCodec.writeServerHandshake(out);
        return new ClientConnection(socket, in, out, name);
    }

    /**
     * Writes one encoded slice event, flushes it, and blocks until the client acknowledges it has applied
     * the event (the per-client half of the barrier). A missing/late/wrong ACK or any I/O error throws so the
     * caller can drop this connection.
     *
     * @param frame   the encoded slice event
     * @param timeout maximum time to wait for the ACK, in milliseconds
     */
    void sendAndAwaitAck(byte[] frame, int timeout) throws IOException {
        socket.setSoTimeout(timeout);
        out.write(frame);
        out.flush();
        final byte ack = in.readByte();
        if (ack != MessageCodec.ACK) {
            throw new IOException("Unexpected acknowledgement byte from client " + name + ": " + ack);
        }
    }

    void closeQuietly() {
        try {
            socket.close();
        } catch (IOException e) {
            Logger.getInstance().debug("Error closing client connection " + name + ": " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "ClientConnection{name=" + name + ", remote=" + socket.getRemoteSocketAddress() + "}";
    }
}
