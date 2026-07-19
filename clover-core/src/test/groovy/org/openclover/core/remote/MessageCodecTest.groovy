package org.openclover.core.remote

import org.junit.Test
import org.openclover.runtime.ErrorInfo
import org.openclover.runtime.remote.MessageCodec
import org.openclover.runtime.remote.RpcMessage
import org_openclover_runtime.Clover

import java.lang.reflect.Method

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * Test for the distributed-coverage wire protocol {@link MessageCodec}.
 *
 * Groovy can invoke the package-private codec members directly at runtime.
 */
class MessageCodecTest {

    private static DataInputStream reader(byte[] bytes) {
        new DataInputStream(new ByteArrayInputStream(bytes))
    }

    @Test
    void testEncodeSliceStart() {
        final RpcMessage msg = RpcMessage.createMethodStart("test.Type", 7, 123456789L)
        final DataInputStream input = reader(MessageCodec.encode(msg))

        assertEquals(MessageCodec.OP_SLICE_START, input.readByte())
        assertEquals("test.Type", input.readUTF())
        assertEquals(7, input.readInt())
        assertEquals(123456789L, input.readLong())
        assertEquals(0, input.available())
    }

    @Test
    void testEncodeSliceEndWithAllFields() {
        final ErrorInfo ei = new ErrorInfo("boom", "at Foo.bar")
        final RpcMessage msg = RpcMessage.createMethodEnd("test.Type", "testMethod", "My Test Name", 3, 9, ei)
        final DataInputStream input = reader(MessageCodec.encode(msg))

        assertEquals(MessageCodec.OP_SLICE_END, input.readByte())
        assertEquals("test.Type", input.readUTF())
        assertTrue(input.readBoolean()); assertEquals("testMethod", input.readUTF())      // method present
        assertTrue(input.readBoolean()); assertEquals("My Test Name", input.readUTF())    // runtimeTestName present
        assertEquals(3, input.readInt())
        assertEquals(9, input.readInt())
        assertTrue(input.readBoolean())                                                   // ErrorInfo present
        assertTrue(input.readBoolean()); assertEquals("boom", input.readUTF())            // message present
        assertTrue(input.readBoolean()); assertEquals("at Foo.bar", input.readUTF())      // stackTrace present
        assertEquals(0, input.available())
    }

    @Test
    void testEncodeSliceEndWithNulls() {
        final RpcMessage msg = RpcMessage.createMethodEnd("test.Type", null, null, 1, 0, null)
        final DataInputStream input = reader(MessageCodec.encode(msg))

        assertEquals(MessageCodec.OP_SLICE_END, input.readByte())
        assertEquals("test.Type", input.readUTF())
        assertFalse(input.readBoolean())    // method absent
        assertFalse(input.readBoolean())    // runtimeTestName absent
        assertEquals(1, input.readInt())
        assertEquals(0, input.readInt())
        assertFalse(input.readBoolean())    // ErrorInfo absent
        assertEquals(0, input.available())
    }

    @Test
    void testNullableUtfKeepsNullDistinctFromEmpty() {
        final ByteArrayOutputStream nullBytes = new ByteArrayOutputStream()
        MessageCodec.writeNullableUTF(new DataOutputStream(nullBytes), null)
        assertNull(MessageCodec.readNullableUTF(reader(nullBytes.toByteArray())))

        final ByteArrayOutputStream emptyBytes = new ByteArrayOutputStream()
        MessageCodec.writeNullableUTF(new DataOutputStream(emptyBytes), "")
        assertEquals("", MessageCodec.readNullableUTF(reader(emptyBytes.toByteArray())))
    }

    @Test
    void testErrorInfoRoundTripWithNullFields() {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        MessageCodec.writeErrorInfo(new DataOutputStream(bytes), new ErrorInfo(null, null))

        final ErrorInfo decoded = MessageCodec.readErrorInfo(reader(bytes.toByteArray()))
        assertNull(decoded.getMessage())
        assertNull(decoded.getStackTrace())
    }

    @Test
    void testHandshakeRoundTrip() {
        final ByteArrayOutputStream clientBytes = new ByteArrayOutputStream()
        MessageCodec.writeClientHandshake(new DataOutputStream(clientBytes))
        MessageCodec.readClientHandshake(reader(clientBytes.toByteArray()))   // must not throw

        final ByteArrayOutputStream serverBytes = new ByteArrayOutputStream()
        MessageCodec.writeServerHandshake(new DataOutputStream(serverBytes))
        MessageCodec.readServerHandshake(reader(serverBytes.toByteArray()))   // must not throw
    }

    @Test
    void testBadMagicRejected() {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream out = new DataOutputStream(bytes)
        out.writeInt(0xDEADBEEF as int)
        out.writeInt(MessageCodec.VERSION)
        out.writeUTF("intruder")
        try {
            MessageCodec.readClientHandshake(reader(bytes.toByteArray()))
            fail("Expected IOException on bad magic")
        } catch (IOException expected) {
        }
    }

    @Test
    void testBadVersionRejected() {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream out = new DataOutputStream(bytes)
        out.writeInt(MessageCodec.MAGIC)
        out.writeInt(MessageCodec.VERSION + 999)
        try {
            MessageCodec.readServerHandshake(reader(bytes.toByteArray()))
            fail("Expected IOException on version mismatch")
        } catch (IOException expected) {
        }
    }

    @Test
    void testUnknownOpcodeNeverDispatches() {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        new DataOutputStream(bytes).writeByte(99)
        try {
            MessageCodec.decodeAndDispatch(reader(bytes.toByteArray()))
            fail("Expected IOException on unknown opcode")
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unknown opcode"))
        }
    }

    /**
     * Drift guard: the codec hardcodes exactly these two Clover method signatures. If either changes, this
     * fails and forces {@link MessageCodec} to be updated in step.
     */
    @Test
    void testWhitelistedCloverSignaturesExist() {
        final Method start = Clover.class.getMethod("allRecordersSliceStart",
                String.class, int.class, long.class)
        assertEquals(void.class, start.getReturnType())

        final Method end = Clover.class.getMethod("allRecordersSliceEnd",
                String.class, String.class, String.class, int.class, int.class, ErrorInfo.class)
        assertEquals(void.class, end.getReturnType())
    }

    @Test
    void testEncodeStartAndEndAreDistinctOpcodes() {
        final byte[] start = MessageCodec.encode(RpcMessage.createMethodStart("t", 1, 1L))
        final byte[] end = MessageCodec.encode(RpcMessage.createMethodEnd("t", "m", "n", 1, 0, null))
        assertEquals(MessageCodec.OP_SLICE_START, start[0])
        assertEquals(MessageCodec.OP_SLICE_END, end[0])
        assertFalse(Arrays.equals(start, end))
    }
}
