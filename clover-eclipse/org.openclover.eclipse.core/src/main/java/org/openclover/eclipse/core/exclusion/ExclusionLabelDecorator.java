package org.openclover.eclipse.core.exclusion;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.ui.PlatformUI;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.ui.CloverPluginIcons;

public class ExclusionLabelDecorator implements ILightweightLabelDecorator {

    private static final int QUADRANT = IDecoration.TOP_RIGHT;

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof ICloverExcludable
                && CloverPlugin.getInstance().getInstallationSettings().isShowExclusionAnnotations()) {
            final ICloverExcludable excludable = (ICloverExcludable) element;
            if (ExclusionUtil.isPresent(excludable, true)) {
                decoration.addOverlay(CloverPlugin.getImageDescriptor(CloverPluginIcons.CLOVER_OVL_GRAY_ICON), QUADRANT);
            } else if (ExclusionUtil.isPresent(excludable, false)) {
                decoration.addOverlay(CloverPlugin.getImageDescriptor(CloverPluginIcons.CLOVER_OVL_GREEN_ICON), QUADRANT);
            } else if (excludable.isLeaf()) {
                IResource res = excludable.getJavaElement().getResource();
                CloverProject cloverProject;
                try {
                    cloverProject = CloverProject.getFor(excludable.getJavaElement().getJavaProject());
                } catch (CoreException e) {
                    cloverProject = null;
                }
                if (cloverProject != null && res instanceof IFile) {
                    final boolean excluded = new ExclusionFilter(cloverProject.getSettings()).isFilteredOut((IFile) res);
                    decoration.addOverlay(CloverPlugin.getImageDescriptor(excluded ? CloverPluginIcons.CLOVER_OVL_GRAY_ICON
                            : CloverPluginIcons.CLOVER_OVL_GREEN_ICON), QUADRANT);
                }
            }
        }
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    /**
     * Call when exclusion/inclusion changed and the overlays need to be
     * redrawn.
     * <p>
     * Must be called from UI thread.
     */
    public static void decorationChanged() {
        PlatformUI.getWorkbench().getDecoratorManager().update("org.openclover.eclipse.core.exclusionDecorator");
    }

}
