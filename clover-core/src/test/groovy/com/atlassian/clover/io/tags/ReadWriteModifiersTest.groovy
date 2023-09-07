package com.atlassian.clover.io.tags

import com.atlassian.clover.registry.entities.AnnotationImpl
import com.atlassian.clover.registry.entities.ArrayAnnotationValue
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.registry.entities.StringifiedAnnotationValue
import org.junit.Test

import java.lang.reflect.Modifier

import static org.junit.Assert.assertEquals

/**
 * Test for serialization and deserialization of {@link Modifiers} class
 */
class ReadWriteModifiersTest {
    @Test
    void testModifiers() throws IOException {
        // configure writer
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream()
        final Tags tags = new Tags()
                .registerTag(Modifiers.class.getName(), Tags.NEXT_TAG, new ObjectReader<Modifiers>() {
                    Modifiers read(TaggedDataInput input) throws IOException {
                        return Modifiers.read(input)
                    }
                }).registerTag(AnnotationImpl.class.getName(), Tags.NEXT_TAG + 1, new ObjectReader<AnnotationImpl>() {
                    AnnotationImpl read(TaggedDataInput input) throws IOException {
                        return AnnotationImpl.read(input)
                    }
                }).registerTag(ArrayAnnotationValue.class.getName(), Tags.NEXT_TAG + 2, new ObjectReader<ArrayAnnotationValue>() {
                    ArrayAnnotationValue read(TaggedDataInput input) throws IOException {
                        return ArrayAnnotationValue.read(input)
                    }
                }).registerTag(StringifiedAnnotationValue.class.getName(), Tags.NEXT_TAG + 3, new ObjectReader<StringifiedAnnotationValue>() {
                    StringifiedAnnotationValue read(TaggedDataInput input) throws IOException {
                        return StringifiedAnnotationValue.read(input)
                    }
                })
        final TaggedOutputWriter out = new TaggedOutputWriter(new DataOutputStream(bytes), tags)

        // write few objects having strings inside and also a null string
        AnnotationImpl greetingAnnotation = new AnnotationImpl("Greeting")
        greetingAnnotation.put("message", new StringifiedAnnotationValue("Hello World"))
        Modifiers writtenModifiers1 = Modifiers.createFrom(Modifier.PUBLIC | Modifier.STATIC,
                [ greetingAnnotation ] as AnnotationImpl[])
        Modifiers writtenModifiers2 = new Modifiers()

        out.write(Modifiers.class, writtenModifiers1)
        out.write(Modifiers.class, writtenModifiers2)

        // configure reader
        final TaggedInputReader input = new TaggedInputReader(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),
                tags)
        final Modifiers readModifiers1 = input.read(Modifiers.class)
        final Modifiers readModifiers2 = input.read(Modifiers.class)

        // compare what we've got
        assertEquals(writtenModifiers1.toString(), readModifiers1.toString())
        assertEquals(writtenModifiers2.toString(), readModifiers2.toString())
    }
}
