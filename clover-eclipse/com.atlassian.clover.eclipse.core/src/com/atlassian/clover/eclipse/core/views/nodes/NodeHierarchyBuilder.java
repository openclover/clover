package com.atlassian.clover.eclipse.core.views.nodes;

public abstract class NodeHierarchyBuilder {

    /** @return children or null to indicate delegation is required */
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        NodeRelationship[] relationships = getNodeRelationships();
        for (NodeRelationship relationship : relationships) {
            if (relationship.includes(object)) {
                return relationship.getChildren(object, filter);
            }
        }
        return null;
    }

    /** @return if object has children or null to indicate delegation is required */
    public Boolean hasChildren(Object object, NodeRelationshipFilter filter) {
        NodeRelationship[] relationships = getNodeRelationships();
        for (NodeRelationship relationship : relationships) {
            if (relationship.includes(object)) {
                return relationship.hasChildren(object, filter);
            }
        }
        return null;
    }

    /** @return elements or null to indicate delegation is required */
    public Object[] getElements(Object object, NodeRelationshipFilter filter) {
        NodeRelationship[] relationships = getNodeRelationships();
        for (NodeRelationship relationship : relationships) {
            if (relationship.includes(object)) {
                return relationship.getElements(object, filter);
            }
        }
        return null;
    }

    public abstract NodeRelationship[] getNodeRelationships();
}
