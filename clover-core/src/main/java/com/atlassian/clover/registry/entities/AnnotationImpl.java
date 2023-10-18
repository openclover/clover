package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.Annotation;
import com.atlassian.clover.api.registry.AnnotationValue;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.registry.AnnotationValueCollection;
import com.atlassian.clover.registry.PersistentAnnotationValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openclover.util.Maps.newHashMap;

/**
 * Represents a Java 1.5 source annotation. Annotation have a name and have attributes (name and value pairs).
 * Annotations can also the values of annotation attributes.
 */
public class AnnotationImpl implements Annotation, PersistentAnnotationValue, AnnotationValueCollection {
    @NotNull
    private String name = "";

    @NotNull
    private Map<String, PersistentAnnotationValue> attributes = newHashMap();

    /**
     * Create an annotation with no name and no attributes. Helper constructor
     */
    public AnnotationImpl() { }

    /**
     * Create an annotation with a specified name.
     * @param name name
     */
    public AnnotationImpl(@NotNull String name) {
        this.name = name;
    }

    /**
     * Create new annotation by copying another one
     * @param annotation source
     */
    public AnnotationImpl(Annotation annotation) {
        name = annotation.getName();
        for (Map.Entry<String, ? extends AnnotationValue> entry : annotation.getAttributes().entrySet()) {
            // class cast - assuming that we've got ArrayAnnotationValue or StringifiedAnnotationValue only in
            // the source annotation
            put(entry.getKey(), (PersistentAnnotationValue)entry.getValue());
        }
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public Map<String, AnnotationValue> getAttributes() {
        return Collections.<String, AnnotationValue>unmodifiableMap(attributes);
    }

    @Override
    public void put(String attributeName, PersistentAnnotationValue value) {
        attributes.put(attributeName, value);
    }

    @Override
    @Nullable
    public AnnotationValue getAttribute(@NotNull String attributeName) {
        return attributes.get(attributeName);
    }

    @Override
    @NotNull
    public List<? extends AnnotationValue> toList() {
        return Collections.singletonList(this);
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeUTF(name);
        final Set<Map.Entry<String, PersistentAnnotationValue>> entries = attributes.entrySet();
        out.writeInt(entries.size());
        for (Map.Entry<String, PersistentAnnotationValue> entry : entries) {
            out.writeUTF(entry.getKey());
            out.write(entry.getValue().getClass(), entry.getValue());
        }
    }

    public static AnnotationImpl read(TaggedDataInput in) throws IOException {
        AnnotationImpl annotation = new AnnotationImpl(in.readUTF());
        final int numEntries = in.readInt();
        for(int i = 0; i < numEntries; i++) {
            annotation.put(in.readUTF(), in.read(PersistentAnnotationValue.class));
        }
        return annotation;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "AnnotationImpl{" +
            "name='" + name + '\'' +
            ", attributes=" + attributes +
            '}';
    }
    ///CLOVER:ON
}