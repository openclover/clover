package org.openclover.eclipse.core.reports;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.CloveredProjectLabelProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.model.WorkbenchContentProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

/**
 *
 */
public class SelectProjectsPage extends WizardPage {
    private Table projectsTable;
    private TableViewer projectsTableViewer;
    private CloverProject initialProject;
    private CloverProject[] selectedProjects = new CloverProject[] {};

    public SelectProjectsPage(CloverProject project) {
        super("SelectProjects");
        initialProject = project;
        setTitle("Project Selection");
        setDescription("Select one or more projects to report on.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        new Label(composite, SWT.NONE).setText("Clover-enabled projects:");
        projectsTable = new Table(composite, SWT.CHECK | SWT.BORDER);
        projectsTable.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        projectsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                calcSelectedProjects();
                getWizard().getContainer().updateButtons();
            }
        });
        projectsTableViewer = new TableViewer(projectsTable);
        projectsTableViewer.setContentProvider(new WorkbenchContentProvider() {
            @Override
            public Object[] getChildren(Object parent) {
                if (parent instanceof IProject) {
                    return null;
                } else if (parent instanceof IWorkspace) {
                    java.util.List<IProject> projects = newArrayList(((IWorkspace) parent).getRoot().getProjects()); // copy
                    for(Iterator<IProject> iter = projects.iterator(); iter.hasNext();) {
                        try {
                            if (!CloverProject.isAppliedTo(iter.next())) {
                                iter.remove();
                            }
                        } catch (CoreException e) {
                            iter.remove();
                        }
                    }
                    return projects.toArray();
                } else {
                    return super.getChildren(parent);
                }
            }
        });
        projectsTableViewer.setLabelProvider(new CloveredProjectLabelProvider());
        projectsTableViewer.setInput(ResourcesPlugin.getWorkspace());

        if (initialProject != null) {
            TableItem[] items = projectsTable.getItems();
            for (TableItem item : items) {
                if (item.getData() == initialProject.getProject()) {
                    item.setChecked(true);
                    selectedProjects = new CloverProject[]{initialProject};
                    break;
                }
            }
        }
        
        setControl(composite);
    }

    private void calcSelectedProjects() {
        TableItem[] items = projectsTable.getItems();
        List projects = new ArrayList(items.length);
        for (TableItem item : items) {
            if (item.getChecked()) {
                try {
                    projects.add(CloverProject.getFor((IProject) item.getData()));
                } catch (CoreException e) {
                    CloverPlugin.logError("Unable to coerce project into Clover project", e);
                }
            }
        }
        selectedProjects = (CloverProject[])projects.toArray(new CloverProject[projects.size()]);
    }

    @Override
    public boolean canFlipToNextPage() {
        boolean oneSelected = false;
        TableItem[] items = projectsTable.getItems();
        for (TableItem item : items) {
            oneSelected |= item.getChecked();
            if (oneSelected) {
                break;
            }
        }
        return oneSelected;
    }

    @Override
    public IWizardPage getNextPage() {
        GenerateReportWizard wizard = (GenerateReportWizard) getWizard();
        if (wizard.selectReportPage.isHtmlSelected()) {
            return wizard.configureHtmlPage;
        } else if (wizard.selectReportPage.isPdfSelected()) {
            return wizard.configurePdfPage;
        } else if (wizard.selectReportPage.isXmlSelected()) {
            return wizard.configureXmlPage;
        } else {
            return null;
        }
    }

    public CloverProject[] getSelectedProjects() {
        return selectedProjects;
    }
}
