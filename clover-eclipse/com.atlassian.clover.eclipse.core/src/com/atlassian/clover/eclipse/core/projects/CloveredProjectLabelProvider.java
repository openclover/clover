package com.atlassian.clover.eclipse.core.projects;

import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.core.resources.IProject;

public class CloveredProjectLabelProvider extends WorkbenchLabelProvider {
    @Override
    protected ImageDescriptor decorateImage(ImageDescriptor image, Object object) {
        if (object instanceof IProject) {
            return CloveredProjectImageDescriptor.imageDescriptorFor((IProject)object, image);
        } else {
            return super.decorateImage(image, object);
        }
    }

}
