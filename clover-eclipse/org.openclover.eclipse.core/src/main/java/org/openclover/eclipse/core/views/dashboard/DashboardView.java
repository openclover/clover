package org.openclover.eclipse.core.views.dashboard;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.openclover.core.CloverDatabase;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.builder.PathUtils;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeEvent;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeListener;
import org.openclover.runtime.api.CloverException;

import java.io.File;
import java.io.IOException;

import static org.openclover.eclipse.core.CloverPlugin.logError;
import static org.openclover.eclipse.core.CloverPlugin.logWarning;

public class DashboardView extends ViewPart implements ISelectionListener, DatabaseChangeListener {

    public static final String ID = CloverPlugin.ID + ".views.dashboard";
    private static final String EMPTY_DASHBOARD = "<body style=\"font-family:verdana,arial,sans-serif; font-size:12px;\">Please select a Cloverized project.</body>";

    private Browser browser;
    CloverProject lastSelectedProject;
    
    @Override
    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(browser);
        
        browser.addLocationListener(new DashboardLocationListener(this));
        browser.setText(EMPTY_DASHBOARD);
        
        selectionChanged(null, getSite().getWorkbenchWindow().getSelectionService().getSelection());
    }
    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        site.getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
        CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(this);
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        String projectName = memento != null ? memento.getString("lastSelectedProject") : null;
        if (projectName != null) {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            try {
                lastSelectedProject = CloverProject.getFor(project);
            } catch (CoreException e) {
                logWarning("Exception while restoring Dashboard view", e);
            }
        }
    }
    
    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        if (lastSelectedProject != null) {
            memento.putString("lastSelectedProject", lastSelectedProject.getName());
        }
    }
    
    @Override
    public void dispose() {
        CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(this);
        getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(this);
        super.dispose();
    }
    
    void projectSelected(IProject project) throws CloverException, IOException, Exception {
        final CloverProject cloverProject = CloverProject.getFor(project);
        if (cloverProject != null && cloverProject != lastSelectedProject) {
            generateReport(null, cloverProject);
        }
    }
    
    protected void generateReport(IProgressMonitor monitor, CloverProject cloverProject) throws Exception, CloverException, IOException {
        CloverDatabase database;
        if ( (cloverProject != null) && ((database = cloverProject.getModel().getDatabase()) != null) ) {
            final DashboardGenerator dashboardGenerator = new DashboardGenerator(
                    database,
                    ensureReportFolderCreated(cloverProject));
            dashboardGenerator.execute();
            browser.setUrl(dashboardGenerator.getDashboardURL());
            lastSelectedProject = cloverProject;
        } else {
            browser.setText(EMPTY_DASHBOARD);
        }
    }

    protected File ensureReportFolderCreated(CloverProject project) throws CoreException {
        IFolder folder = project.getReportDir();
        if (!folder.exists()) {
            PathUtils.makeDerivedFolder(folder);
        }
        return folder.getLocation().toFile();
    }


    @Override
    public void setFocus() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            final Object firstElement = ((IStructuredSelection)selection).getFirstElement();
            if (firstElement instanceof IAdaptable) {
                final IAdaptable adaptable = (IAdaptable)firstElement;
                final IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                if (resource != null) {
                    try {
                        projectSelected(resource.getProject());
                    } catch (Exception e) {
                        logError("Error creating the dashboard", e);
                    }
                } else {
                    final IJavaElement element = (IJavaElement) adaptable.getAdapter(IJavaElement.class);
                    IJavaProject javaProject = element != null ? element.getJavaProject() : null; 
                    try {
                        projectSelected(javaProject != null ? javaProject.getProject() : null);
                    } catch (Exception e) {
                        logError("Error creating the dashboard", e);
                    }
                }
            }
        }
    }
    
    @Override
    public void databaseChanged(final DatabaseChangeEvent event) {
        browser.getDisplay().asyncExec(() -> {
            if ((event.isApplicableTo(lastSelectedProject) && event.isSubstantiveProjectChange()) || event.isForWorkspace()) {
                try {
                    generateReport(null, lastSelectedProject);
                } catch (Exception e) {
                    logWarning("Problem generating Dashboard report", e);
                }
            }
        });
    }    
}
