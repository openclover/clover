package com.atlassian.clover.eclipse.core.ui.projects.widgets;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.projects.CloveredProjectLabelProvider;
import com.atlassian.clover.eclipse.core.ui.CloverPluginIcons;
import com.atlassian.clover.eclipse.core.ui.SwtUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.model.WorkbenchContentProvider;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Sets.newHashSet;

public class ToggleCloverProjectsDialog extends Dialog {
    private static final int CLOVER_PROJ_WARN_THRESHOLD = 4;

    private Table projectTable;
    private TableViewer projectTableViewer;
    private Set projectsToToggle = newHashSet();
    private LocalResourceManager resourceManager;
    private Composite warningComposite;
    private Label prompt;

    public ToggleCloverProjectsDialog(Shell shell) {
        super(shell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Enable/Disable Clover on Projects");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        resourceManager = new LocalResourceManager(JFaceResources.getResources());
        Composite body = new Composite(parent, SWT.NONE) {
            @Override
            public void dispose() {
                super.dispose();
                resourceManager.dispose();
            }
        };
        body.setLayoutData(new GridData(GridData.FILL_BOTH));
        body.setLayout(new GridLayout(3, false));
        ((GridLayout) body.getLayout()).horizontalSpacing = 10;
        ((GridLayout) body.getLayout()).verticalSpacing = 10;
        ((GridLayout) body.getLayout()).marginWidth = 10;
        ((GridLayout) body.getLayout()).marginHeight = 10;

        warningComposite = new Composite(body, SWT.BORDER);
        warningComposite.setLayout(new GridLayout(2, false));
        warningComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
        SwtUtils.gridDataFor(warningComposite).horizontalSpan = 3;
        warningComposite.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        warningComposite.setVisible(false);

        Label warningImage = new Label(warningComposite, SWT.NONE);
        warningImage.setImage(
            CloverPluginIcons.grabPluginImage(
                resourceManager,
                "org.eclipse.ui",
                "icons/full/obj16/warn_tsk.gif"));
        SwtUtils.gridDataFor(warningImage).verticalAlignment = SWT.TOP;
        warningImage.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        Label warningLabel = new Label(warningComposite, SWT.WRAP);
        warningLabel.setText(
            "Enabling Clover on large projects or lots of medium size projects consumes additional memory. We recommend " +
            "you incrementally enable Clover a few projects at a time until you have " +
            "determined the maximum number your current memory settings can handle.");
        warningLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        warningLabel.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        prompt = new Label(body, SWT.NONE);
        prompt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.gridDataFor(prompt).horizontalSpan = 3;
        prompt.setText("Select the projects Clover should track code coverage for:");

        SwtUtils.gridDataFor(warningComposite).widthHint = (int)(prompt.computeSize(SWT.DEFAULT, SWT.DEFAULT).x * 1.5);
        
        projectTable = new Table(body, SWT.CHECK | SWT.BORDER);
        projectTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        SwtUtils.gridDataFor(projectTable).horizontalSpan = 3;
        SwtUtils.gridDataFor(projectTable).heightHint = 150;
        SwtUtils.gridDataFor(projectTable).widthHint = 200;
        projectTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                updateWarning(true);
            }
        });

        projectTableViewer = new TableViewer(projectTable);
        projectTableViewer.setContentProvider(new WorkbenchContentProvider() {
            @Override
            public Object[] getChildren(Object parent) {
                if (parent instanceof IProject) {
                    return null;
                } else if (parent instanceof IWorkspace) {
                    List projects = newArrayList(((IWorkspace) parent).getRoot().getProjects()); // copy
                    for (Iterator iter = projects.iterator(); iter.hasNext();) {
                        IProject project = (IProject) iter.next();
                        if (!project.isAccessible()
                            || (JavaCore.create(project) == null)
                            || project.getName().indexOf(".") == 0) {
                            iter.remove();
                        }
                    }
                    return projects.toArray();
                } else {
                    return super.getChildren(parent);
                }
            }
        });
        projectTableViewer.setLabelProvider(new CloveredProjectLabelProvider());
        projectTableViewer.setInput(ResourcesPlugin.getWorkspace());

        TableItem[] items = projectTable.getItems();
        for (TableItem item : items) {
            try {
                if (CloverProject.isAppliedTo((IProject) item.getData())) {
                    item.setChecked(true);
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Error while checking Clover-enabled and disabled projects", e);
            }
        }

        Label filler = new Label(body, SWT.NONE);
        filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button selectAllButton = new Button(body, SWT.NONE);
        selectAllButton.setText("Select all");
        SwtUtils.gridDataFor(selectAllButton).horizontalAlignment = GridData.END;
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                setAllChecked(true);
            }
        });

        Button deselectAllButton = new Button(body, SWT.NONE);
        deselectAllButton.setText("Deselect all");
        SwtUtils.gridDataFor(deselectAllButton).horizontalAlignment = GridData.END;
        deselectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                setAllChecked(false);                
            }
        });

        updateWarning(false);

        return body;
    }

    private void setAllChecked(boolean checked) {
        TableItem[] projectItems = projectTable.getItems();
        for (TableItem projectItem : projectItems) {
            projectItem.setChecked(checked);
        }
        updateWarning(true);
    }

    @Override
    protected void okPressed() {
        TableItem[] projectItems = projectTable.getItems();
        for (TableItem projectItem : projectItems) {
            try {
                IProject project = (IProject) projectItem.getData();
                boolean checked = projectItem.getChecked();
                if ((CloverProject.isAppliedTo(project) && !checked)
                        || (!CloverProject.isAppliedTo(project) && checked)) {
                    projectsToToggle.add(project);
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Error determing projects to enable/disable", e);
            }
        }
        super.okPressed();
    }

    public Set getProjectsToToggle() {
        return projectsToToggle;
    }


    private void updateWarning(boolean layout) {
        if (getCheckedCount() > CLOVER_PROJ_WARN_THRESHOLD) {
            showWarning(layout);
        } else {
            hideWarning(layout);
        }
    }

    private int getCheckedCount() {
        int checked = 0;
        final TableItem[] items = projectTable.getItems();
        for (TableItem item : items) {
            if (item.getChecked()) {
                checked++;
            }
        }
        return checked;
    }

    private void showWarning(boolean layout) {
        SwtUtils.gridDataFor(warningComposite).exclude = false;
        if (layout) {
            ((Composite)getContents()).layout(true, true);
        }
        warningComposite.setVisible(true);
    }

    private void hideWarning(boolean layout) {
        SwtUtils.gridDataFor(warningComposite).exclude = true;
        if (layout) {
            ((Composite)getContents()).layout(true, true);
        }
        warningComposite.setVisible(false);
    }
}
