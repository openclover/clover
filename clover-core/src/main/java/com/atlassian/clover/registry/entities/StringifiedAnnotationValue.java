package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.AnnotationValue;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.registry.PersistentAnnotationValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * An annotation value that is not an array of annotation values and is not an annotation.
 * The value is stored as a stringified version of the original expression. E.g.
 * the annotation value of @Foo(1==1?true:false) will be "1==1?true:false". The
 * annotation values of the array annotation value of the implicit annotation attribute "value"
 * of @Foo(1, "hello, world!") will be "1" and "\"hello, world!\"". I need a beer.
  */
public class StringifiedAnnotationValue implements PersistentAnnotationValue {
    public String value;

    public StringifiedAnnotationValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    @NotNull
    public List<? extends AnnotationValue> toList() {
        return Collections.singletonList(this);
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeUTF(value);
    }

    public static StringifiedAnnotationValue read(TaggedDataInput in) throws IOException {
        return new StringifiedAnnotationValue(in.readUTF());
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return value;
    }
    ///CLOVER:ON
}
