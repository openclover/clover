package com.atlassian.clover.eclipse.core.projects;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.ui.CloverPluginIcons;

public abstract class CloveredProjectImageDescriptor extends CompositeImageDescriptor {
    private final ImageDescriptor originalImage;

    protected CloveredProjectImageDescriptor(ImageDescriptor originalImage) {
        this.originalImage = originalImage;
    }

    @Override
    protected void drawCompositeImage(int width, int height) {
        drawImage(originalImage.getImageData(), 0, 0);
        drawImage(getOverlayImageData(), 0, 0);
    }

    protected abstract ImageData getOverlayImageData();

    @Override
    protected Point getSize() {
        return new Point(originalImage.getImageData().width, originalImage.getImageData().height);
    }

    /** Important - required so we do not leak resources in WorkbenchLabelProvider */
    @Override
    public boolean equals(Object obj) {
        return
            obj != null
            && obj.getClass() == getClass()
            && ((CloveredProjectImageDescriptor)obj).originalImage.equals(originalImage);
    }

    /** Important - required so we do not leak resources in WorkbenchLabelProvider */
    @Override
    public int hashCode() {
        return originalImage.hashCode() ^ 7;
    }

    public static ImageDescriptor imageDescriptorFor(IProject project, ImageDescriptor imageDescriptor) {
        try {
            if (CloverProject.isAppliedTo(project)) {
                CloverProject cloverProject = CloverProject.getFor(project);
                if (!cloverProject.getSettings().isInstrumentationEnabled()) {
                    return new NoInstrumentationState(imageDescriptor);
//TODO:
//                } else if (cloverProject.getModel().didFailLoading()) {
//                    return new ErrorState(imageDescriptor);
                } else {
                    return new OkState(imageDescriptor);
                }
            }
        } catch (CoreException e) {
            CloverPlugin.logError("Error determining Clovered project icon", e);
        }

        return imageDescriptor;
    }
    
    private static class OkState extends CloveredProjectImageDescriptor {
        public OkState(ImageDescriptor originalImage) {
            super(originalImage);
        }

        @Override
        protected ImageData getOverlayImageData() {
            return CloverPlugin.getImage(CloverPluginIcons.CLOVERED_OVERLAY_ICON).getImageData();
        }
    }

    private static class ErrorState extends CloveredProjectImageDescriptor {
        private ErrorState(ImageDescriptor originalImage) {
            super(originalImage);
        }

        @Override
        protected ImageData getOverlayImageData() {
            return CloverPlugin.getImage(CloverPluginIcons.CLOVERED_ERROR_OVERLAY_ICON).getImageData();
        }
    }

    private static class NoInstrumentationState extends CloveredProjectImageDescriptor {
        private NoInstrumentationState(ImageDescriptor originalImage) {
            super(originalImage);
        }

        @Override
        protected ImageData getOverlayImageData() {
            return CloverPlugin.getImage(CloverPluginIcons.CLOVERED_NO_COMPILE_OVERLAY_ICON).getImageData();
        }
    }
}
