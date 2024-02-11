package org.openclover.core.io.tags

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class TagsTest {
    private TaggedPersistent readResult = new TaggedPersistent() {
        ///CLOVER:OFF
        void write(TaggedDataOutput out) throws IOException {
        }
        ///CLOVER:ON
    }

    @Test
    void testCanRegisterTagAndReadTag() throws IOException {
        final boolean[] read = [ false ] as boolean[]
        final ObjectReader<TaggedPersistent> reader = new ObjectReader<TaggedPersistent>() {
            TaggedPersistent read(TaggedDataInput input) throws IOException {
                read[0] = true
                return readResult
            }
        }
        final Tags tags = new Tags().registerTag(readResult.getClass().getName(), Tags.NEXT_TAG, reader)

        final ByteArrayOutputStream baos = new ByteArrayOutputStream()
        final DataOutputStream out = new DataOutputStream(baos)
        out.writeInt(Tags.NEXT_TAG)
        out.flush()

        assertTrue(tags.isDefined(Tags.NEXT_TAG))
        assertEquals(Tags.NEXT_TAG, tags.getTagFor(readResult.getClass()))
        assertSame(tags.invokeObjectReaderFor(Tags.NEXT_TAG, new TaggedInputReader(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())), tags)), readResult)
    }

    @Test
    void testUnknownTagThrowsException() throws IOException {
        try {
            new Tags().getTagFor(readResult.getClass())
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (UnknownTagException e) {
            //pass
        }

        try {
            new Tags().invokeObjectReaderFor(Tags.NEXT_TAG, null)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (UnknownTagException e) {
            //pass
        }
    }

    @Test
    void testInvalidTagThrowsException() {
        try {
            new Tags().registerTag("Foo", Tags.NEXT_TAG - 1, null)
            fail()
        } catch (IllegalArgumentException e) {
            //pass
        }

        try {
            new Tags().registerTag("Foo", 0, null)
            fail()
        } catch (IllegalArgumentException e) {
            //pass
        }

        try {
            new Tags().registerTag("Foo", Byte.MAX_VALUE + 1, null)
            fail()
        } catch (IllegalArgumentException e) {
            //pass
        }
    }
}
