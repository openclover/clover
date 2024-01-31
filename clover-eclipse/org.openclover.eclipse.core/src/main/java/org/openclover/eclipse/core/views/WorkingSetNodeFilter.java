package org.openclover.eclipse.core.views;

import org.openclover.eclipse.core.views.nodes.NodeRelationshipFilter;
import org.openclover.eclipse.core.CloverPlugin;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;

public class WorkingSetNodeFilter implements NodeRelationshipFilter {
    private boolean inWorkingSetMode = CloverPlugin.getInstance().isInWorkingSetMode();

    @Override
    public Object[] perform(Collection elements) {
        if (requiresFiltering()) {
            elements.removeIf(o -> !accept(o));
        }
        return elements.toArray();
    }

    @Override
    public boolean requiresFiltering() {
        return inWorkingSetMode;
    }

    @Override
    public boolean accept(Object element) {
        return
            !inWorkingSetMode
                || (element instanceof IAdaptable
                    && CloverPlugin.getInstance().getCloverWorkingSet().includes((IAdaptable)element));
    }
}
