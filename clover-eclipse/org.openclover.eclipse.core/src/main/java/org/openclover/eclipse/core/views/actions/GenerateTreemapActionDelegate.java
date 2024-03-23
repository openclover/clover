package org.openclover.eclipse.core.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ide.IDE;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.ui.editors.treemap.TreemapEditor;
import org.openclover.eclipse.core.ui.editors.treemap.TreemapInput;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class GenerateTreemapActionDelegate extends GenerateReportletActionDelegate {
    @Override
    public void run(IAction action) {
        final IProject project = (IProject) projects.iterator().next();
        Display.getDefault().syncExec(() -> {
            try {
                IDE.openEditor(
                    getPage(),
                    new TreemapInput(CloverProject.getFor(project)),
                    TreemapEditor.ID);
            } catch (Throwable t) {
                logError("Unable to open treemap editor", t);
            }
        });
    }
}
