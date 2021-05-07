package org.openclover.eclipse.core.ui.editors.cloud;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.views.actions.OpenJavaEditorAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;

public class EditorLinkingLocationListener implements LocationListener {
    public static final String JAVAEDITOR_HREF_PREFIX = "#javaeditor:";

    private CloverProject project;
    private OpenJavaEditorAction action;
    public static final LocationListener NO_LINKING = new LocationListener() {
        @Override
        public void changing(LocationEvent locationEvent) { }

        @Override
        public void changed(LocationEvent locationEvent) { }
    };

    public EditorLinkingLocationListener(CloverProject project, OpenJavaEditorAction action) {
        this.action = action;
        this.project = project;
    }

    @Override
    public void changing(LocationEvent locationEvent) {
        CloverPlugin.logVerbose("Browser link click: " + locationEvent.location);
        int pos = locationEvent.location.indexOf(JAVAEDITOR_HREF_PREFIX);
        if (pos != -1) {
            String className = locationEvent.location.substring(pos + JAVAEDITOR_HREF_PREFIX.length());

            try {
                IType type = project.getJavaProject().findType(className, (IProgressMonitor)null);
                if (type != null) {
                    action.run(new StructuredSelection(type));
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to open source for cloud link: " + locationEvent.location, e);
            }
        }
    }

    @Override
    public void changed(LocationEvent locationEvent) {
        //No impl
        }
    }
