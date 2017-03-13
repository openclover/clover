package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.Annotation;
import com.atlassian.clover.api.registry.ModifiersInfo;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import static clover.com.google.common.collect.Maps.newHashMap;


/**
 * container class for modifiers and annotations
 */
public class Modifiers implements TaggedPersistent, ModifiersInfo {

    public static Modifiers createFrom(int modifiers, @Nullable AnnotationImpl[] annotations) {
        final Modifiers result = new Modifiers();
        result.setMask(modifiers);
        if (annotations != null) {
            for (AnnotationImpl annotation : annotations) {
                result.addAnnotation(annotation);
            }
        }
        return result;
    }

    public Modifiers() {

    }

    public Modifiers(@Nullable ModifiersInfo modifiers) {
        if (modifiers != null) {
            mask = modifiers.getMask();
            // double loop because of repeated annotations (having the same name but different attributes)
            for (Collection<Annotation> annotationsLists : modifiers.getAnnotations().values()) {
                for (Annotation annotation : annotationsLists) {
                    addAnnotation(new AnnotationImpl(annotation));
                }
            }
        }
    }

    private int mask = 0;

    /**
     * List of annotation objects. Key is an annotation name, value contains list of annotations (and their attributes)
     * with that name. Note that since JDK1.8 it is possible to define more than one annotation with the same name for
     * given code element.
     */
    @NotNull
    private Map<String, Collection<Annotation>> annotations = newHashMap();

    public boolean containsAnnotation(@NotNull String name) {
        return annotations.containsKey(name);
    }

    public boolean containsAnnotation(@NotNull Pattern pattern) {
        for (String annotationName : annotations.keySet()) {
            if (pattern.matcher(annotationName).matches()) {
                return true;
            }
        }

        return false;
    }

    @Override
    @NotNull
    public Collection<Annotation> getAnnotation(@NotNull String name) {
        Collection<Annotation> value = annotations.get(name);
        return value != null ? Collections.unmodifiableCollection(value) : Collections.<Annotation>emptyList();
    }

    @Override
    @NotNull
    public Map<String, Collection<Annotation>> getAnnotations() {
        return Collections.unmodifiableMap(annotations);
    }

    public void addAnnotation(AnnotationImpl annotation) {
        // fetch existing values for this name or create new collection
        Collection<Annotation> values = annotations.get(annotation.getName());
        if (values == null) {
            values = new ArrayList<Annotation>(1); // size=1 because assuming that repeating annotations are rarely used
        }
        values.add(annotation);

        // add or update existing key
        annotations.put(annotation.getName(), values);
    }

    public void clearAnnotations() {
        annotations.clear();
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    public String getVisibility() {
        int visibilityMods = mask & (Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC);
        if (visibilityMods == 0) {
            return "package";
        }
        return Modifier.toString(visibilityMods);
    }

    @Override
    public void write(final TaggedDataOutput out) throws IOException {
        out.writeInt(mask);

        // count all annotations (we can have repeating annotations)
        int size = 0;
        for (final Collection<Annotation> annotationCollection : annotations.values()) {
            size += annotationCollection.size();
        }
        out.writeInt(size);

        // write all annotations flattening the map
        for (final Collection<Annotation> annotationCollection : annotations.values()) {
            for (final Annotation annotation : annotationCollection) {
                out.write(AnnotationImpl.class, (AnnotationImpl)annotation);
            }
        }
    }

    public static Modifiers read(final TaggedDataInput in) throws IOException {
        final Modifiers result = new Modifiers();
        result.setMask(in.readInt());
        final int entriesSize = in.readInt();
        for (int i = 0; i < entriesSize; i++) {
            // no worries about un-flattening, the addAnnotation will handle repeated annotations
            result.addAnnotation(in.read(AnnotationImpl.class));
        }
        return result;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "Modifiers{" +
            "modifiers=" + Modifier.toString(mask) +
            ", annotations=" + annotations +
            '}';
    }
    ///CLOVER:ON
}
