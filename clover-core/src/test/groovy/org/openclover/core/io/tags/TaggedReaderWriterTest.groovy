package org.openclover.core.io.tags

import com.atlassian.clover.io.tags.ObjectReader
import com.atlassian.clover.io.tags.TaggedDataInput
import com.atlassian.clover.io.tags.TaggedDataOutput
import com.atlassian.clover.io.tags.TaggedInputReader
import com.atlassian.clover.io.tags.TaggedOutputWriter
import com.atlassian.clover.io.tags.TaggedPersistent
import com.atlassian.clover.io.tags.Tags
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * Test for {@link TaggedOutputWriter#writeList(Class, java.util.List)} and
 * {@lik TaggedInputReader#readList}
 */
public class TaggedReaderWriterTest {

    static class MyClass implements TaggedPersistent {
        private String text;

        MyClass(String text) {
            this.text = text;
        }
        public void write(TaggedDataOutput out) throws IOException {
            out.writeUTF(text);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MyClass &&
                    ( (text == null && ((MyClass)other).text == null)
                            || text.equals(((MyClass)other).text) );
        }
    }

    @Test
    public void testReadWriteList() throws IOException {
        // configure writer
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Tags tags = new Tags().registerTag(
                MyClass.class.getName(),
                Tags.NEXT_TAG,
                new ObjectReader<MyClass>() {
                    ///CLOVER:OFF
                    public MyClass read(TaggedDataInput input) throws IOException {
                        return new MyClass(input.readUTF());
                    }
                    ///CLOVER:ON
                });
        final TaggedOutputWriter out = new TaggedOutputWriter(new DataOutputStream(bytes), tags);

        // write few objects having strings inside and also a null string
        List<MyClass> writtenElements = [ new MyClass("abc"), new MyClass("def"), new MyClass(null), new MyClass("ghi") ]
        out.writeInt(100);
        out.writeList(MyClass.class, writtenElements);
        out.writeInt(200);

        // configure reader
        final TaggedInputReader input = new TaggedInputReader(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),
                tags);
        int i100 = input.readInt();
        List<MyClass> readElements = input.readList(MyClass.class);
        int i200 = input.readInt();

        // compare what we've got
        assertEquals(100, i100);
        assertEquals(200, i200);
        assertEquals(4, readElements.size());
        assertEquals(writtenElements, readElements);
    }
}
