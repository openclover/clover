package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public interface ModifiersInfo {
    long getMask();

    /**
     * Returns one (or more - introduced in Java 8) annotations with a given name.
     *
     * @param name name of the annotation to search
     * @return Collection&lt;Annotation&gt; list of annotations with that name or empty list if not found
     */
    @NotNull
    Collection<Annotation> getAnnotation(@NotNull String name);

    /**
     * Returns a multi-map of all annotations - one key (annotation name) may contain mutliple values
     * (i.e. multiple occurrences of the annotation, usually with different attributes)
     *
     * @return  Map&lt;String, Collection&lt;Annotation&gt;&gt; - map(name, annotations)
     */
    @NotNull
    Map<String, Collection<Annotation>> getAnnotations();
}
