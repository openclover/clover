package org.openclover.core.api.registry;

/**
 * Represents an entity for which their internal elements can be visited (walking a tree).
 * For example, you can visit classes in a file, or statements in a method.
 */
public interface IsVisitable {

    void visitElements(ElementVisitor visitor);

}
