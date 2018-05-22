package com.atlassian.clover.io.tags

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class TaggedOutputWriterTest {

    static class MyTaggedObject implements TaggedPersistent {
        private boolean wrote = false
        void write(TaggedDataOutput out) throws IOException {
            wrote = true
        }
    }

    @Test
    void testCanWriteRawValues() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream daos = new DataOutputStream(bytes)
        final TaggedOutputWriter out = new TaggedOutputWriter(daos, new Tags())
        out.writeBoolean(true)
        out.writeByte(0)
        out.writeChar((int)('a' as char))
        out.writeDouble(1.0d)
        out.writeFloat(1.0f)
        out.writeInt(1)
        out.writeLong(1l)
        out.writeShort(1)
        out.writeUTF("abc")
        out.writeUTF(null)
        daos.flush()

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(dis.readByte(), Tags.BOOL_TAG)
        assertEquals(dis.readBoolean(), true)
        assertEquals(dis.readByte(), Tags.BYTE_TAG)
        assertEquals(dis.readByte(), 0)
        assertEquals(dis.readByte(), Tags.CHAR_TAG)
        assertEquals(dis.readChar(), 'a' as char)
        assertEquals(dis.readByte(), Tags.DOUBLE_TAG)
        assertEquals(dis.readDouble(), 1.0d, 0.01d)
        assertEquals(dis.readByte(), Tags.FLOAT_TAG)
        assertEquals(dis.readFloat(), 1.0f, 0.01f)
        assertEquals(dis.readByte(), Tags.INT_TAG)
        assertEquals(dis.readInt(), 1)
        assertEquals(dis.readByte(), Tags.LONG_TAG)
        assertEquals(dis.readLong(), 1l)
        assertEquals(dis.readByte(), Tags.SHORT_TAG)
        assertEquals(dis.readShort(), 1)
        assertEquals(dis.readByte(), Tags.STRING_TAG)
        assertEquals(dis.readBoolean(), false)
        assertEquals(dis.readUTF(), "abc")
        assertEquals(dis.readByte(), Tags.STRING_TAG)
        assertEquals(dis.readBoolean(), true)
    }

    @Test
    void testCanWriteObject() throws IOException {
        final MyTaggedObject myTaggedObject = new MyTaggedObject()
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream daos = new DataOutputStream(bytes)
        final TaggedOutputWriter out = new TaggedOutputWriter(daos, new Tags().registerTag(MyTaggedObject.class.getName(), Tags.NEXT_TAG, new Tags.ObjectReader<MyTaggedObject>() {
            ///CLOVER:OFF
            MyTaggedObject read(TaggedDataInput input) throws IOException {
                return null
            }
            ///CLOVER:ON
        }))

        out.write(MyTaggedObject.class, myTaggedObject)
        out.write(MyTaggedObject.class, null)
        daos.flush()

        assertTrue(myTaggedObject.wrote)

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(dis.readByte(), Tags.NEXT_TAG)
        assertEquals(dis.readBoolean(), false)
        assertEquals(dis.readByte(), Tags.NEXT_TAG)
        assertEquals(dis.readBoolean(), true)
    }

    @Test
    void testWritingUnknownClassCausesException() throws IOException {
        try {
            new TaggedOutputWriter(new DataOutputStream(new ByteArrayOutputStream()), new Tags().registerTag("foo", Tags.NEXT_TAG, new Tags.ObjectReader<TaggedPersistent>() {
                ///CLOVER:OFF
                TaggedPersistent read(TaggedDataInput input) throws IOException {
                    return null
                }
                ///CLOVER:ON
            })).write(TaggedPersistent.class, null)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (UnknownTagException e) {
            //pass
        }
    }
}
