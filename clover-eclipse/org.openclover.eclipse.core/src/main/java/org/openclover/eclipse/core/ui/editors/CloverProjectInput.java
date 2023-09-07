package org.openclover.eclipse.core.ui.editors;

import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public abstract class CloverProjectInput implements IEditorInput {

    private CloverProject project;

    public CloverProjectInput(CloverProject project) {
        this.project = project;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return project.getProject().getName();
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return "";
    }

    @Override
    public Object getAdapter(Class aClass) {
        return null;
    }

    public boolean equals(Object object) {
        return
            object == this
                || ((object != null)
                    && object.getClass() == getClass()
                    && project.equals(((CloverProjectInput)object).project));
    }

    public int hashCode() {
        return project.hashCode();
    }

    public CloverProject getProject() {
        return project;
    }
}
