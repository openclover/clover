package org.openclover.eclipse.core.exclusion;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.settings.InstallationSettings;

public class ToggleShowAnnotationViewDelegate implements IViewActionDelegate {

    @Override
    public void init(IViewPart view) {
    }

    @Override
    public void run(IAction action) {
        final InstallationSettings installationSettings = CloverPlugin.getInstance().getInstallationSettings();
        installationSettings.setShowExclusionAnnotations(!installationSettings.isShowExclusionAnnotations());
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setChecked(CloverPlugin.getInstance().getInstallationSettings().isShowExclusionAnnotations());
    }

}
