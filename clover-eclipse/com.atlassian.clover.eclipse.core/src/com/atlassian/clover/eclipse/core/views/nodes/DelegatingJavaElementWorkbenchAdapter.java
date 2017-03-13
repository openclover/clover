package com.atlassian.clover.eclipse.core.views.nodes;

import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Allows Eclipse to display instances of JavaElementNode using the nice icons
 * etc even though it isn't an instance IJavaElement.
 */
public class DelegatingJavaElementWorkbenchAdapter implements IWorkbenchAdapter {
    public static final DelegatingJavaElementWorkbenchAdapter INSTANCE =
        new DelegatingJavaElementWorkbenchAdapter();

    /** No children */
    @Override
    public Object[] getChildren(Object o) {
        return new Object[] {};
    }

    private IJavaElement toJavaElement(Object o) {
        return ((JavaElementNode)o).toJavaElement();
    }

    /** Delegates to IType's image */
    @Override
    public ImageDescriptor getImageDescriptor(Object o) {
        IJavaElement javaElement = toJavaElement(o);
        final IWorkbenchAdapter workbenchAdapter = (IWorkbenchAdapter)javaElement.getAdapter(IWorkbenchAdapter.class);
        return
            workbenchAdapter == null
                ? null
                : workbenchAdapter.getImageDescriptor(javaElement);
    }

    /** Delegates to IType's label */
    @Override
    public String getLabel(Object o) {
        IJavaElement javaElement = toJavaElement(o);
        final IWorkbenchAdapter workbenchAdapter = (IWorkbenchAdapter)javaElement.getAdapter(IWorkbenchAdapter.class);
        return
            workbenchAdapter == null
                ? null
                : workbenchAdapter.getLabel(javaElement);
    }

    /** Um, not sure what's correct here */
    @Override
    public Object getParent(Object o) {
        IJavaElement javaElement = toJavaElement(o);
        final IWorkbenchAdapter workbenchAdapter = (IWorkbenchAdapter)javaElement.getAdapter(IWorkbenchAdapter.class);
        return
            workbenchAdapter == null
                ? null
                : workbenchAdapter.getParent(javaElement);
    }
}
