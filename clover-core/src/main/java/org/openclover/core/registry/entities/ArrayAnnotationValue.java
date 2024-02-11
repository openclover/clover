package org.openclover.core.registry.entities;

import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.api.registry.AnnotationValue;
import org.openclover.core.registry.AnnotationValueCollection;
import org.openclover.core.registry.PersistentAnnotationValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

/**
 * An annotation value that is an ordered collection of annotation values.
 */
public class ArrayAnnotationValue implements PersistentAnnotationValue, AnnotationValueCollection {
    private List<PersistentAnnotationValue> values = newArrayList();

    public ArrayAnnotationValue() {
    }

    public ArrayAnnotationValue(PersistentAnnotationValue[] annotationValues) {
        values.addAll(Arrays.asList(annotationValues));
    }

    /**
     * Adds an annotation value to the array.
     * @param key ignored
     * @param value the annotation value
     */
    @Override
    public void put(String key, PersistentAnnotationValue value) {
        values.add(value);
    }

    public List<PersistentAnnotationValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    @Override
    @NotNull
    public List<? extends AnnotationValue> toList() {
        return getValues();
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeInt(values.size());
        for (PersistentAnnotationValue value : values) {
            out.write(value.getClass(), value);
        }
    }

    public static ArrayAnnotationValue read(TaggedDataInput in) throws IOException{
        ArrayAnnotationValue result = new ArrayAnnotationValue();
        final int valueSize = in.readInt();
        for(int i = 0; i < valueSize; i++) {
            result.put(null, in.read(PersistentAnnotationValue.class));
        }
        return result;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "ArrayAnnotationValue{" +
            "values=" + values +
            '}';
    }
    ///CLOVER:ON
}
