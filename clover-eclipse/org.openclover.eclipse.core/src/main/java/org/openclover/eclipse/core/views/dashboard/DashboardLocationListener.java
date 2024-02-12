/**
 *
 */
package org.openclover.eclipse.core.views.dashboard;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditor;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.ui.editors.cloud.CloudEditor;
import org.openclover.eclipse.core.ui.editors.cloud.CloudProjectInput;
import org.openclover.eclipse.core.views.actions.GenerateCloudJob;
import org.openclover.eclipse.core.views.coverageexplorer.CoverageView;
import org.openclover.eclipse.core.views.testrunexplorer.TestRunExplorerView;

import java.net.URI;
import java.net.URISyntaxException;

final class DashboardLocationListener extends LocationAdapter {
    /**
     *
     */
    private final DashboardView dashboardView;

    /**
     * @param dashboardView
     */
    DashboardLocationListener(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
    }

    @Override
    public void changing(LocationEvent event) {
        CloverPlugin.logVerbose("Changing: " + event.location);
        URI uri;
        try {
            uri = new URI(event.location);
        } catch (final URISyntaxException e) {
            // event.location can be a file path (i.e. without 'file:/' prefix) when loading a dashboard (event sent by Browser)
            CloverPlugin.logVerbose("Unexpected URI in the Dashboard view: " + event.location, e);
            return;
        }
        final String scheme = uri.getScheme();
        try {
            if (DashboardHtmlRenderingSupport.JAVA_CLASS_SCHEME.equals(scheme)
                    || DashboardHtmlRenderingSupport.JAVA_METHOD_SCHEME.equals(scheme)) {
                handleJavaSrcLocation(event, uri);
            } else if (DashboardHtmlRenderingSupport.JAVA_PACKAGE_SCHEME.equals(scheme)) {
                handleJavaPackageLocation(event, uri);
            } else if ("cloverview".equals(scheme)) {
                handleCloverView(event, uri);
            }
        } catch (final Exception e) {
            CloverPlugin.logWarning("Error opening location " + event.location, e);
        }

    }

    private void handleCloverView(LocationEvent event, URI uri) throws CoreException {
        event.doit = false;
        final String kind = uri.getSchemeSpecificPart();

        if ("toprisks".equals(kind)) {
            openCloudEditor(dashboardView.lastSelectedProject.getProject());
        } else if ("quickwins".equals(kind)) {
            openCloudEditor(dashboardView.lastSelectedProject.getProject());
        } else if ("treemap".equals(kind)) {
            // unsupported so far
        } else if ("coverage".equals(kind)) {
            focusCoverageExplorerOn(dashboardView.lastSelectedProject.getJavaProject());
        } else if ("testresults".equals(kind)) {
            final IWorkbenchPage page = this.dashboardView.getSite().getPage();
            page.showView(TestRunExplorerView.ID);
        }
    }

    private void openCloudEditor(final IProject project) {
        new GenerateCloudJob(project) {

            @Override
            protected IStatus activateEditor() {
                final IStatus[] openEditorStatus = new IStatus[] {Status.OK_STATUS};

                Display.getDefault().syncExec(() -> {
                    try {
                        IDE.openEditor(
                            dashboardView.getSite().getPage(),
                        new CloudProjectInput(CloverProject.getFor(project)),
                            CloudEditor.ID);
                    } catch (final Throwable t) {
                        openEditorStatus[0] = new Status(Status.ERROR, CloverPlugin.ID, 0, CloverEclipsePluginMessages.FAILED_TO_OPEN_CLOUD_EDITOR(), t);
                    }
                });
                return openEditorStatus[0];
            }
        }.schedule();
    }

    private void handleJavaPackageLocation(LocationEvent event, URI uri) throws CoreException {
        event.doit = false;
        final String relativePath = uri.getSchemeSpecificPart().replace('.', '/');

        final IJavaElement element = this.dashboardView.lastSelectedProject.getJavaProject().findElement(
                new Path(relativePath));

        if (element instanceof IPackageFragment) {
            focusCoverageExplorerOn(element);
        }

    }

    private void focusCoverageExplorerOn(final IJavaElement element) throws PartInitException {
        final IWorkbenchPage page = this.dashboardView.getSite().getPage();
        final IViewPart view = page.showView(CoverageView.ID);
        if (view instanceof IShowInTarget) {
            final IShowInTarget showInTarget = (IShowInTarget) view;
            final ShowInContext context = new ShowInContext(null, new StructuredSelection(element));
            showInTarget.show(context);
        }
    }

    private void handleJavaSrcLocation(LocationEvent event, URI uri) throws JavaModelException, CoreException,
            BadLocationException {
        event.doit = false;
        final String relativePath = uri.getPath().substring(1);
        final String[] locations = uri.getFragment().split("_");
        final int line = locations.length > 0 ? Integer.parseInt(locations[0]) - 1 : 0;
        final int column = locations.length > 1 ? Integer.parseInt(locations[1]) - 1 : 0;

        final IJavaElement element = this.dashboardView.lastSelectedProject.getJavaProject().findElement(
                new Path(relativePath));
        final Object resource = (element instanceof ICompilationUnit) ? ((ICompilationUnit) element)
                .getCorrespondingResource() : null;
        if (resource instanceof IFile) {
            final IEditorPart editor = IDE.openEditor(this.dashboardView.getSite().getPage(), (IFile) resource);
            if (editor instanceof ITextEditor) {
                final ITextEditor textEditor = (ITextEditor) editor;
                final IDocument doc = textEditor.getDocumentProvider().getDocument(editor.getEditorInput());
                final int offset = doc.getLineOffset(line) + column;
                ((ITextEditor) editor).selectAndReveal(offset, 0);
            }
            final IShowInSource showInSource = (IShowInSource) editor.getAdapter(IShowInSource.class);
            if (showInSource != null) {
                final IWorkbenchPage page = this.dashboardView.getSite().getPage();
                final IViewPart view = page.showView(CoverageView.ID);
                if (view instanceof IShowInTarget) {
                    final IShowInTarget showInTarget = (IShowInTarget) view;
                    final ShowInContext context = showInSource.getShowInContext();
                    showInTarget.show(context);
                    page.activate(editor);
                }
            }
        }
    }
}