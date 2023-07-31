package org.openclover.eclipse.core.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import java.util.Iterator;
import java.util.Set;

import static org.openclover.util.Sets.newHashSet;

public class SelectionUtils {
    private SelectionUtils() {
    }

    public static Set<IProject> gatherProjectsForSelection(ISelection selection) {
        final Set<IProject> projects = newHashSet();
        if (selection instanceof IStructuredSelection)
        {
            for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
                IJavaElement selectedJavaElement = asJavaElement(iter.next());
                if (selectedJavaElement != null) {
                    projects.add(selectedJavaElement.getJavaProject().getProject());
                }
            }
        }
        return projects;
    }

    public static IJavaElement asJavaElement(Object element) {
        IJavaElement javaElement = null;
        if (element instanceof IJavaElement) {
            javaElement = (IJavaElement)element;
        } else {
            if (element instanceof IAdaptable) {
                javaElement = (IJavaElement)((IAdaptable) element).getAdapter(IJavaElement.class);
            }
        }
        return javaElement;
    }
}
