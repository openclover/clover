package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.AnnotationValue;
import com.atlassian.clover.io.tags.TaggedPersistent;

/**
 * An AnnotationValue which is can be persisted using TaggedPersistent.write()
 */
public interface PersistentAnnotationValue extends AnnotationValue, TaggedPersistent {

}
