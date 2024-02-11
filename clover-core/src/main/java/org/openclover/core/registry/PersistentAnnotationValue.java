package org.openclover.core.registry;

import org.openclover.core.api.registry.AnnotationValue;
import org.openclover.core.io.tags.TaggedPersistent;

/**
 * An AnnotationValue which is can be persisted using TaggedPersistent.write()
 */
public interface PersistentAnnotationValue extends AnnotationValue, TaggedPersistent {

}
