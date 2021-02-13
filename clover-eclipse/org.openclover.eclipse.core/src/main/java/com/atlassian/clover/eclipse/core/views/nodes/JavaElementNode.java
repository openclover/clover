package com.atlassian.clover.eclipse.core.views.nodes;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.eclipse.jdt.core.IJavaElement;

public abstract class JavaElementNode implements IAdaptable {
    @Override
    public Object getAdapter(Class clazz) {
        return
            IWorkbenchAdapter.class == clazz
                ? getWorkbenchAdapter()
                : IJavaElement.class == clazz
                    ? toJavaElement()
                    : ITaskListResourceAdapter.class == clazz
                      ? getTaskListResourceAdapter()
                      : delegateAdaption(clazz);
    }

    //CLOV-785 - CCEs in other parts of Eclipse because our JavaElementNodes aren't IJavaElements.
    //Seems like faulty logic in JavaTaskListAdapter & MarkerView which expect all selections to be this way
    //Or could be user error, for values of user { MS }
    private ITaskListResourceAdapter getTaskListResourceAdapter() {
        return new ITaskListResourceAdapter() {
            @Override
            public IResource getAffectedResource(IAdaptable iAdaptable) {
                return (IResource)getAdapter(IResource.class);
            }
        };
    }

    private Object delegateAdaption(Class clazz) {
        final IJavaElement javaElement = toJavaElement();
        return
            javaElement == null
                ? null
                : javaElement.getAdapter(clazz);
    }

    protected IWorkbenchAdapter getWorkbenchAdapter() {
        return DelegatingJavaElementWorkbenchAdapter.INSTANCE;
    }

    public abstract IJavaElement toJavaElement();
}
