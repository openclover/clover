package org.openclover.runtime.remote;

import org.openclover.runtime.ErrorInfo;
import org_openclover_runtime.Clover;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The distributed-coverage wire protocol. This class is the whole remote surface: a tiny, closed
 * set of messages exchanged over a raw TCP socket using {@link DataInputStream}/{@link DataOutputStream},
 * with no object serialization at all.
 * <p/>
 * The {@link #decodeAndDispatch(DataInputStream)} switch is the security whitelist: an unknown opcode is
 * rejected and there is no path from wire bytes to an arbitrary method or class. The only methods it can
 * ever call are {@link Clover#allRecordersSliceStart(String, int, long)} and
 * {@link Clover#allRecordersSliceEnd(String, String, String, int, int, ErrorInfo)}.
 *
 * @see RpcMessage
 */
final class MessageCodec {

    /** Fixed marker ("CLVR") exchanged in the handshake so a stray/non-Clover peer fails fast. */
    static final int MAGIC = 0x434C5652;

    /**
     * Protocol version. Bump this on any wire-format change so mismatched OpenClover versions
     * fail fast at the handshake instead of misinterpreting bytes.
     */
    static final int VERSION = 1;

    static final byte OP_SLICE_START = 1;
    static final byte OP_SLICE_END = 2;

    /** Single byte a client writes back once it has applied a slice event (the barrier acknowledgement). */
    static final byte ACK = 0x06;

    private MessageCodec() {
    }

    // --- handshake (once, on connect) ---

    /** client&rarr;server: MAGIC, VERSION, client name. */
    static void writeClientHandshake(DataOutputStream out, String name) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeUTF(name == null ? "" : name);
        out.flush();
    }

    /** server side: validate the client handshake and return the client name; throws on mismatch. */
    static String readClientHandshake(DataInputStream in) throws IOException {
        readMagicAndVersion(in, "client");
        return in.readUTF();
    }

    /** server&rarr;client: MAGIC, VERSION reply, proving a real Clover server answered. */
    static void writeServerHandshake(DataOutputStream out) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.flush();
    }

    /** client side: validate the server reply; throws on mismatch. */
    static void readServerHandshake(DataInputStream in) throws IOException {
        readMagicAndVersion(in, "server");
    }

    private static void readMagicAndVersion(DataInputStream in, String peer) throws IOException {
        final int magic = in.readInt();
        if (magic != MAGIC) {
            throw new IOException("Bad magic from " + peer + ": 0x" + Integer.toHexString(magic)
                    + " (not an OpenClover distributed-coverage peer)");
        }
        final int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Unsupported protocol version from " + peer + ": " + version
                    + " (expected " + VERSION + ")");
        }
    }

    // --- slice events (server -> client) ---

    /**
     * Encodes a single slice event into a self-delimiting frame. Reads the arguments in the fixed,
     * declared order for the message's opcode; never serializes the {@link RpcMessage} object itself.
     */
    static byte[] encode(RpcMessage message) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(baos);
        switch (message.getMethodId()) {
            case RpcMessage.METHOD_START:
                encodeSliceStart(out, message.getMethodArgs());
                break;
            case RpcMessage.METHOD_END:
                encodeSliceEnd(out, message.getMethodArgs());
                break;
            default:
                throw new IOException("Unknown opcode: " + message.getMethodId());
        }
        out.flush();
        return baos.toByteArray();
    }

    /** allRecordersSliceStart(String type, int slice, long startTime) */
    private static void encodeSliceStart(DataOutputStream out, Object[] args) throws IOException {
        out.writeByte(OP_SLICE_START);
        out.writeUTF((String) args[0]);
        out.writeInt((Integer) args[1]);
        out.writeLong((Long) args[2]);
    }

    /** allRecordersSliceEnd(String type, String method, String runtimeTestName, int slice, int p, ErrorInfo ei) */
    private static void encodeSliceEnd(DataOutputStream out, Object[] args) throws IOException {
        out.writeByte(OP_SLICE_END);
        out.writeUTF((String) args[0]);
        writeNullableUTF(out, (String) args[1]);
        writeNullableUTF(out, (String) args[2]);
        out.writeInt((Integer) args[3]);
        out.writeInt((Integer) args[4]);
        writeErrorInfo(out, (ErrorInfo) args[5]);
    }

    /**
     * Reads exactly one slice event and applies it locally by calling the whitelisted {@link Clover} method.
     * An unknown opcode throws {@link IOException} (the caller closes the connection); no other method is reachable.
     */
    static void decodeAndDispatch(DataInputStream in) throws IOException {
        final byte opcode = in.readByte();
        switch (opcode) {
            case OP_SLICE_START:
                dispatchSliceStart(in);
                break;
            case OP_SLICE_END:
                dispatchSliceEnd(in);
                break;
            default:
                throw new IOException("Unknown opcode: " + opcode);
        }
    }

    private static void dispatchSliceStart(DataInputStream in) throws IOException {
        final String type = in.readUTF();
        final int slice = in.readInt();
        final long startTime = in.readLong();
        Clover.allRecordersSliceStart(type, slice, startTime);
    }

    private static void dispatchSliceEnd(DataInputStream in) throws IOException {
        final String type = in.readUTF();
        final String method = readNullableUTF(in);
        final String runtimeTestName = readNullableUTF(in);
        final int slice = in.readInt();
        final int p = in.readInt();
        final ErrorInfo ei = readErrorInfo(in);
        Clover.allRecordersSliceEnd(type, method, runtimeTestName, slice, p, ei);
    }

    // --- helpers ---

    /** Writes a nullable string as a present-flag byte followed by the UTF payload when present. */
    static void writeNullableUTF(DataOutputStream out, String s) throws IOException {
        out.writeBoolean(s != null);
        if (s != null) {
            out.writeUTF(s);
        }
    }

    /** Reads a {@link #writeNullableUTF} value; keeps {@code null} distinct from {@code ""}. */
    static String readNullableUTF(DataInputStream in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }

    /** Writes an optional {@link ErrorInfo} as a present-flag byte plus two nullable UTF fields. */
    static void writeErrorInfo(DataOutputStream out, ErrorInfo ei) throws IOException {
        out.writeBoolean(ei != null);
        if (ei != null) {
            writeNullableUTF(out, ei.getMessage());
            writeNullableUTF(out, ei.getStackTrace());
        }
    }

    /** Reads a {@link #writeErrorInfo} value; absent present-flag yields {@code null}. */
    static ErrorInfo readErrorInfo(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        final String message = readNullableUTF(in);
        final String stackTrace = readNullableUTF(in);
        return new ErrorInfo(message, stackTrace);
    }
}
