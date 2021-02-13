package com.atlassian.clover.eclipse.core.ui.editors.java;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import com.atlassian.clover.eclipse.core.settings.InstallationSettings;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.SystemJob;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class EditorCoverageSynchronizer implements IWindowListener, IPartListener2, IDisposable {
    private IWorkbench workbench;
    private final List<AnnotationDisplayListener> annotationDisplayListeners;
    private volatile boolean started;

    public EditorCoverageSynchronizer(IWorkbench workbench) {
        this.workbench = workbench;
        this.annotationDisplayListeners = newArrayList();
        this.started = false;
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            window.getPartService().addPartListener(this);
        }
        workbench.addWindowListener(this);
    }

    public void syncWithCoverageSetting(int setting) {
        if ((setting == InstallationSettings.Values.SHOW_NO_COVERAGE_IN_EDITORS) && started) {
            stop();
        } else if ((setting != InstallationSettings.Values.SHOW_NO_COVERAGE_IN_EDITORS) && !started) {
            start(setting);
        } else {
            fireAnnotationDisplayChanged();
        }
    }

    private void start(int representationStyle) {
        if (!started && representationStyle != InstallationSettings.Values.SHOW_NO_COVERAGE_IN_EDITORS) {
            started = true;
            IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
            //Apply annotations to all open editors
            for (IWorkbenchWindow window : windows) {
                IWorkbenchPage[] pages = window.getPages();
                for (IWorkbenchPage page : pages) {
                    IEditorReference[] editors = page.getEditorReferences();
                    for (IEditorReference editor : editors) {
                        //If editor is no longer present (dead reference) then
                        //we don't try to resurrect the editor (getPart(false))
                        applyAnnotations(editor.getPart(false));
                    }
                }
            }
        }
    }

    private void stop() {
        Job stopJob = new SystemJob("Removing coverage annotations") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
                //Apply annotations to all open editors
                for (IWorkbenchWindow window : windows) {
                    IWorkbenchPage[] pages = window.getPages();
                    for (IWorkbenchPage page : pages) {
                        IEditorReference[] editors = page.getEditorReferences();
                        for (IEditorReference editor : editors) {
                            //If editor is no longer present (dead reference) then
                            //we don't try to resurrect the editor (getPart(false))
                            removeAnnotations(editor.getPart(false), true);
                        }
                    }
                }
                started = false;
                return Status.OK_STATUS;
            }
        };
        stopJob.schedule();
        try {
            stopJob.join();
        } catch (InterruptedException e) {
            CloverPlugin.logError("Failed to join on coverage synchronizer shutdown job", e);
        }
    }

    public void addAnnotationDisplayListener(AnnotationDisplayListener listener) {
        synchronized (annotationDisplayListeners) {
            annotationDisplayListeners.add(listener);
        }
    }

    public synchronized void removeAnnotationDisplayListener(AnnotationDisplayListener listener) {
        synchronized (annotationDisplayListeners) {
            annotationDisplayListeners.remove(listener);
        }
    }

    public void fireAnnotationDisplayChanged() {
        final List<AnnotationDisplayListener> listeners;
        synchronized (annotationDisplayListeners) {
            listeners = newArrayList(annotationDisplayListeners);
        }

        for (AnnotationDisplayListener listener : listeners) {
            listener.displayOptionChanged();
        }
    }

    @Override
    public void dispose() {
        if (started) {
            stop();
        }
        workbench.removeWindowListener(this);
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        for (IWorkbenchWindow window : windows) {
            window.getPartService().removePartListener(this);
        }
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
        window.getPartService().removePartListener(this);
    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
        window.getPartService().addPartListener(this);
    }

    @Override
    public void partOpened(IWorkbenchPartReference partReference) {
        if (started) {
            applyAnnotations(partReference.getPart(false));
        }
    }

    @Override
    public void partClosed(IWorkbenchPartReference partReference) {
        if (started) {
            removeAnnotations(partReference.getPart(false), true);
        }
    }

    private void applyAnnotations(final IWorkbenchPart part) {
        if (isJavaEditor(part)) {
            CoverageAnnotationModel.applyTo((ITextEditor)part, this);
        }
    }

    private void removeAnnotations(final IWorkbenchPart part, boolean async) {
        if (isJavaEditor(part)) {
            CoverageAnnotationModel.removeFrom((ITextEditor)part, async);
        }
    }

    private boolean isJavaEditor(IWorkbenchPart part) {
        return
            part instanceof ITextEditor
            && part.getTitle().toLowerCase().contains(".java");
    }

    @Override
    public void windowActivated(IWorkbenchWindow window) {}
    @Override
    public void windowDeactivated(IWorkbenchWindow window) {}
    @Override
    public void partActivated(IWorkbenchPartReference partReference) {}
    @Override
    public void partBroughtToTop(IWorkbenchPartReference partReference) {}
    @Override
    public void partDeactivated(IWorkbenchPartReference partReference) {}
    @Override
    public void partHidden(IWorkbenchPartReference partReference) {}
    @Override
    public void partVisible(IWorkbenchPartReference partReference) {}
    @Override
    public void partInputChanged(IWorkbenchPartReference partReference) {}
}
