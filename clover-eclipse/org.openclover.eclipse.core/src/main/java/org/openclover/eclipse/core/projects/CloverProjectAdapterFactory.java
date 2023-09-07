package org.openclover.eclipse.core.projects;

import org.openclover.eclipse.core.CloverPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaElement;

public class CloverProjectAdapterFactory implements IAdapterFactory {
    /**
     * We adapt other instances to these classes
     */
    private static final Class[] ADAPTED_TO_TYPES = {
        CloverProject.class
    };

    @Override
    public Class[] getAdapterList() {
        return ADAPTED_TO_TYPES;
    }

    @Override
    public Object getAdapter(Object adaptee, Class clazz) {
        try {
            if (clazz == CloverProject.class) {
                if (adaptee instanceof IProject) {
                    return CloverProject.getFor((IProject)adaptee);
                } else if (adaptee instanceof IJavaElement) {
                    return CloverProject.getFor(((IJavaElement)adaptee).getJavaProject());
                }
            }
        } catch (Exception e) {
            CloverPlugin.logError("Error adapting " + adaptee + " to class " + clazz, e);
        }
        return null;
    }
}
