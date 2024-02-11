package org.openclover.core.api.registry;

/**
 * Entity which has it's own parent.
 * For example, a parent of the statement could be a method (OO-languages) or a file (scripting languages).
 */
public interface HasParent {

    EntityContainer getParent();

}
