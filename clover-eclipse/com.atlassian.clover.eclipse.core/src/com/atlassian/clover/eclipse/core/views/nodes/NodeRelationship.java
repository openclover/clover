package com.atlassian.clover.eclipse.core.views.nodes;

public abstract class NodeRelationship {
    public abstract boolean includes(Object object);
    /**
     * WARNING! Ineffecient!
     * If you expect getChildren() to take a long time, consider overriding hasChildren() with a more efficient impl
     * otherwise getChildren() will be called unecessarily to determine if tree nodes can be expanded.
     */
    public Boolean hasChildren(Object object, NodeRelationshipFilter filter) { return getChildren(object, filter).length > 0; }
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) { return null; }
    public Object[] getElements(Object object, NodeRelationshipFilter filter) { return null; }
}
