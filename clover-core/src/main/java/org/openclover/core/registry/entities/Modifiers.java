package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.Annotation;
import org.openclover.core.api.registry.ModifiersInfo;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import static org.openclover.core.util.Maps.newHashMap;


/**
 * container class for modifiers and annotations
 */
public class Modifiers implements TaggedPersistent, ModifiersInfo {

    public static Modifiers createFrom(long modifiers, @Nullable AnnotationImpl[] annotations) {
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

    private long mask = 0;

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

    /**
     * Returns true if the element is annotated with any of the specified annotations.
     */
    public boolean containsAnnotation(@NotNull String... annotationNames) {
        for (String annotationName : annotationNames) {
            if (annotations.containsKey(annotationName)) {
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
            values = new ArrayList<>(1); // size=1 because assuming that repeating annotations are rarely used
        }
        values.add(annotation);

        // add or update existing key
        annotations.put(annotation.getName(), values);
    }

    public void clearAnnotations() {
        annotations.clear();
    }

    public void setMask(long mask) {
        this.mask = mask;
    }

    @Override
    public long getMask() {
        return mask;
    }

    public String getVisibility() {
        long visibilityMods = mask & (Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC);
        if (visibilityMods == 0) {
            return "package";
        }
        return ModifierExt.toString(visibilityMods);
    }

    @Override
    public void write(final TaggedDataOutput out) throws IOException {
        out.writeLong(mask);

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
        result.setMask(in.readLong());
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
            "modifiers=" + ModifierExt.toString(mask) +
            ", annotations=" + annotations +
            '}';
    }
    ///CLOVER:ON
}
