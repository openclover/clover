package com.atlassian.clover.eclipse.core.views.nodes;

import java.util.Collection;

public interface NodeRelationshipFilter {
    public static NodeRelationshipFilter NO_OP = new NodeRelationshipFilter() {
        @Override
        public Object[] perform(Collection elements) {
            return elements.toArray();
        }

        @Override
        public boolean accept(Object element) {
            return true;
        }

        @Override
        public boolean requiresFiltering() {
            return false;
        }
    };
    Object[] perform(Collection elements);
    boolean accept(Object element);
    boolean requiresFiltering();
}
