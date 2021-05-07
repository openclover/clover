package org.openclover.eclipse.core.views.testcontributions;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.services.IDisposable;

public class ActiveEditorListener implements IWindowListener, IPartListener2, IDisposable {
    private final IWorkbench workbench;
    private final TestContributionsView view;

    public ActiveEditorListener(TestContributionsView view) {
        this.view = view;
        this.workbench = view.getSite().getWorkbenchWindow().getWorkbench();

        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            window.getPartService().addPartListener(this);
        }
        workbench.addWindowListener(this);
    }

    @Override
    public void dispose() {
        workbench.removeWindowListener(this);
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            window.getPartService().removePartListener(this);
        }
    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
        window.getPartService().addPartListener(this);
        updateView(window.getPartService().getActivePartReference());
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
        window.getPartService().removePartListener(this);
        view.setEditor(null);
    }

    @Override
    public void partActivated(IWorkbenchPartReference partReference) {
        updateView(partReference);
    }

    private void updateView(IWorkbenchPartReference partReference) {
        if (partReference != null) {
            if (partReference.getPart(false) instanceof ITextEditor) {
                ITextEditor editor = ((ITextEditor) partReference.getPart(false));
                if (editor != null) {
                    view.setEditor(editor);
                }
            }
        }
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partReference) { }
    @Override
    public void partBroughtToTop(IWorkbenchPartReference partReference) { }
    @Override
    public void partClosed(IWorkbenchPartReference partReference) { }
    @Override
    public void partOpened(IWorkbenchPartReference partReference) { }
    @Override
    public void partHidden(IWorkbenchPartReference partReference) { }
    @Override
    public void partVisible(IWorkbenchPartReference partReference) { }
    @Override
    public void partInputChanged(IWorkbenchPartReference partReference) { }
    @Override
    public void windowActivated(IWorkbenchWindow window) { }
    @Override
    public void windowDeactivated(IWorkbenchWindow window) { }
}
