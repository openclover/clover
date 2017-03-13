package com.atlassian.clover.eclipse.core.views;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.ui.projects.DatabaseChangeEvent;
import com.atlassian.clover.eclipse.core.ui.projects.DatabaseChangeListener;
import com.atlassian.clover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import com.atlassian.clover.eclipse.core.views.nodes.NodeRelationshipFilter;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.WorkbenchContentProvider;

import java.util.Set;
import java.util.HashSet;

public abstract class CloveredWorkspaceProvider
    extends WorkbenchContentProvider
    implements IElementChangedListener, DatabaseChangeListener {

    /** Duplicated here as it doesn't exist in Eclipse 3.2 so compilation against 3.2 libs fails */
    private static final int IJAVAELEMENTDELTA_F_CLASSPATH_REORDER = 256;

    protected ExplorerView part;
    protected ExplorerViewSettings settings;
    protected NodeHierarchyBuilder nodeBuilder;

    public CloveredWorkspaceProvider(ExplorerView part, ExplorerViewSettings settings) {
        this.part = part;
        this.settings = settings;
        this.nodeBuilder = settings.nodeBuilderForStyle();
        CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(this);
    }

    @Override
    public Object[] getChildren(Object object) {
        final Object[] result = nodeBuilder.getChildren(object, getFilter());
        return (result == null ? super.getChildren(object) : result);
    }

    @Override
    public boolean hasChildren(Object object) {
        final Boolean result = nodeBuilder.hasChildren(object, getFilter());
        return (result == null ? super.hasChildren(object) : result.booleanValue());
    }

    @Override
    public Object[] getElements(Object parent) {
        final Object[] result = nodeBuilder.getElements(parent, getFilter());
        return (result == null ? super.getElements(parent) : result);
    }

    /**
     * Turns our provider from something that tracks just resource changes to something
     * that tracks Java model changes.
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (oldInput == null && newInput != null) {
            JavaCore.addElementChangedListener(this);
        } else if (oldInput != null && newInput == null) {
            JavaCore.removeElementChangedListener(this);
        }
    }

    private class BreakDeltaProcessing extends Exception {};

    @Override
    public void elementChanged(ElementChangedEvent event) {
        Object[] affectedElements;
        try {
            affectedElements = processDelta(event.getDelta()).toArray();
        } catch (BreakDeltaProcessing breakDeltaProcessing) {
            affectedElements = ExplorerView.ENTIRE_WORKSPACE;
        }
        if (affectedElements.length > 0) {
            part.refresh(true, affectedElements);
        }
    }

    private Set<IJavaProject> processDelta(IJavaElementDelta delta) throws BreakDeltaProcessing {
        return processDelta(new HashSet<IJavaProject>(), delta);
    }

    private Set<IJavaProject> processDelta(Set<IJavaProject> changes, IJavaElementDelta delta) throws BreakDeltaProcessing {
        switch (delta.getKind()) {
            case IJavaElementDelta.ADDED:
            case IJavaElementDelta.REMOVED:
                if (delta.getElement().getElementType() == IJavaElement.JAVA_PROJECT) {
                    throw new BreakDeltaProcessing();
                } else {
                    changes.add(delta.getElement().getJavaProject());
                }
                break;
            case IJavaElementDelta.CHANGED:
                if ((delta.getFlags() & IJavaElementDelta.F_CHILDREN) != 0) {
                    IJavaElementDelta[] children = delta.getAffectedChildren();
                    for (IJavaElementDelta child : children) {
                        processDelta(changes, child);
                    }
                } else if ((delta.getFlags() & IJavaElementDelta.F_OPENED) != 0
                    || (delta.getFlags() & IJavaElementDelta.F_CLOSED) != 0) {
                    throw new BreakDeltaProcessing();
                }
                break;
        }
        return changes;
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent event) {
        if (event.isSubstantiveProjectChange()) {
            part.refresh(true, ExplorerView.ENTIRE_WORKSPACE);
        }
    }

    public void setNodeBuilder(NodeHierarchyBuilder nodeBuilder) {
        this.nodeBuilder = nodeBuilder;
    }

    protected abstract NodeRelationshipFilter getFilter();
}
