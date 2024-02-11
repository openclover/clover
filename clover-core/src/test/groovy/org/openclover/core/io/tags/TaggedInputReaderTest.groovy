package org.openclover.core.io.tags

import com.atlassian.clover.io.tags.ObjectReader
import com.atlassian.clover.io.tags.TagTypeMismatchException
import com.atlassian.clover.io.tags.TaggedDataInput
import com.atlassian.clover.io.tags.TaggedDataOutput
import com.atlassian.clover.io.tags.TaggedInputReader
import com.atlassian.clover.io.tags.TaggedPersistent
import com.atlassian.clover.io.tags.Tags
import com.atlassian.clover.io.tags.UnknownTagException
import com.atlassian.clover.io.tags.WrongTagException
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.fail

class TaggedInputReaderTest {
    
    @Test
    void testCanReadRawValues() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream dos = new DataOutputStream(bytes)
        dos.writeByte(Tags.BOOL_TAG)
        dos.writeBoolean(true)
        dos.writeByte(Tags.BYTE_TAG)
        dos.writeByte(0)
        dos.writeByte(Tags.CHAR_TAG)
        dos.writeChar((int)('a' as char))
        dos.writeByte(Tags.DOUBLE_TAG)
        dos.writeDouble(1.0d)
        dos.writeByte(Tags.FLOAT_TAG)
        dos.writeFloat(1.0f)
        dos.writeByte(Tags.INT_TAG)
        dos.writeInt(1)
        dos.writeByte(Tags.LONG_TAG)
        dos.writeLong(1l)
        dos.writeByte(Tags.SHORT_TAG)
        dos.writeShort(1)
        dos.writeByte(Tags.STRING_TAG)
        dos.writeBoolean(false)
        dos.writeUTF("abc")
        dos.writeByte(Tags.STRING_TAG)
        dos.writeBoolean(true)
        dos.flush()

        final TaggedInputReader input = new TaggedInputReader(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), new Tags())
        assertEquals(input.readBoolean(), true)
        assertEquals(input.readByte(), 0)
        assertEquals(input.readChar(), 'a' as char)
        assertEquals(input.readDouble(), 1.0d, 0.01d)
        assertEquals(input.readFloat(), 1.0f, 0.01f)
        assertEquals(input.readInt(), 1)
        assertEquals(input.readLong(), 1l)
        assertEquals(input.readShort(), 1)
        assertEquals(input.readUTF(), "abc")
        assertNull(input.readUTF())
    }

    @Test
    void testUnexpectedTagsCausesException() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream dos = new DataOutputStream(bytes)
        dos.writeByte(Tags.DOUBLE_TAG)
        dos.writeDouble(1.0d)
        dos.writeByte(Tags.FLOAT_TAG)
        dos.writeFloat(1.0f)
        dos.flush()

        final TaggedInputReader input = new TaggedInputReader(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), new Tags())
        assertEquals(input.readDouble(), 1.0d, 0.01d)
        try {
            input.readUTF()
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (WrongTagException e) {
            //pass
        }
    }

    @Test
    void testCanReadObject() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream dos = new DataOutputStream(bytes)
        dos.writeByte(Tags.NEXT_TAG)
        dos.writeBoolean(false)
        dos.writeByte(Tags.NEXT_TAG)
        dos.writeBoolean(true)
        dos.flush()

        final TaggedInputReader input = new TaggedInputReader(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), new Tags().registerTag(MyTaggedObject.class.getName(), Tags.NEXT_TAG, new ObjectReader<MyTaggedObject>() {
            MyTaggedObject read(TaggedDataInput input) throws IOException {
                return new MyTaggedObject()
            }
        }))
        final MyTaggedObject myTaggedObject = input.read(MyTaggedObject.class)
        final MyTaggedObject myNullTaggedObject = input.read(MyTaggedObject.class)
        assertNotNull(myTaggedObject)
        assertNull(myNullTaggedObject)
    }

    @Test
    void testUnknownTagCausesException() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream dos = new DataOutputStream(bytes)
        dos.writeByte(Tags.NEXT_TAG  + 1)
        dos.writeBoolean(true)
        dos.flush()

        try {
            final TaggedInputReader myReader = new TaggedInputReader(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), new Tags().registerTag(MyTaggedObject.class.getName(), Tags.NEXT_TAG, new ObjectReader<MyTaggedObject>() {
                ///CLOVER:OFF
                MyTaggedObject read(TaggedDataInput input) throws IOException {
                    return new MyTaggedObject()
                }
                ///CLOVER:ON
            }))

            myReader.read(MyTaggedObject.class)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (UnknownTagException e) {
            //pass
        }
    }

    @Test
    void testUnexpectedClassCausesException() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final DataOutputStream dos = new DataOutputStream(bytes)
        dos.writeByte(Tags.NEXT_TAG)
        dos.writeBoolean(false)
        dos.writeByte(Tags.NEXT_TAG  + 1)
        dos.writeBoolean(false)
        dos.flush()

        final TaggedInputReader input =
            new TaggedInputReader(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),
                new Tags().registerTag(MyTaggedObject.class.getName(), Tags.NEXT_TAG, new ObjectReader<MyTaggedObject>() {
                    MyTaggedObject read(TaggedDataInput input) throws IOException {
                        return new MyTaggedObject()
                    }
                }).registerTag(MyTaggedObject2.class.getName(), Tags.NEXT_TAG + 1, new ObjectReader<MyTaggedObject2>() {
                    ///CLOVER:OFF
                    MyTaggedObject2 read(TaggedDataInput input) throws IOException {
                        return new MyTaggedObject2()
                    }
                    ///CLOVER:ON
                }))

        input.read(MyTaggedObject.class)
        try {
            input.read(MyTaggedObject.class)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (TagTypeMismatchException e) {
            //pass
        }
    }

    static class MyTaggedObject implements TaggedPersistent {
        ///CLOVER:OFF
        void write(TaggedDataOutput out) throws IOException {
        }
        ///CLOVER:ON
    }

    static class MyTaggedObject2 implements TaggedPersistent {
        ///CLOVER:OFF
        void write(TaggedDataOutput out) throws IOException {
        }
        ///CLOVER:ON
    }
}
