package org.openclover.eclipse.core.views.actions;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.reports.GenerateReportWizard;
import org.openclover.eclipse.core.reports.OpenReportDialog;
import org.openclover.eclipse.core.reports.OpenReportOperation;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeEvent;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeListener;
import org.openclover.eclipse.core.views.ExplorerView;
import org.openclover.eclipse.core.views.SelectionUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static clover.com.google.common.collect.Sets.newHashSet;

public class GenerateReportAction
    extends Action
    implements IMenuCreator, DatabaseChangeListener, ISelectionChangedListener {

    private final ExplorerView view;
    private Set projects = newHashSet();
    private Menu menu;

    public GenerateReportAction(ExplorerView view) {
        this.view = view;
        this.projects = newHashSet();
        setId("org.openclover.eclipse.core.actions.report.generate");
        setText(CloverEclipsePluginMessages.PROJECT_REPORT());
        setToolTipText(CloverEclipsePluginMessages.PROJECT_REPORT_TOOL_TIP());
        setImageDescriptor(CloverPlugin.getImageDescriptor(CloverPluginIcons.REPORT_ICON));
        setMenuCreator(this);
        updateEnablement();
    }

    @Override
    public int getStyle() {
        return AS_DROP_DOWN_MENU;
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent event) {
        updateEnablement();
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        projects = SelectionUtils.gatherProjectsForSelection(event.getSelection());
    }

    private void updateEnablement() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        boolean shouldShow = false;
        for (IProject project : projects) {
            try {
                shouldShow |= CloverProject.isAppliedTo(project);
            } catch (CoreException e) {
                CloverPlugin.logError("Error checking whether to enable report button", e);
            }
        }
        setEnabled(shouldShow);
    }

    @Override
    public void run() {
        generate();
    }

    public void generate() {
        try {
            CloverProject project = null;
            if (!projects.isEmpty()) {
                project = CloverProject.getFor((IProject) projects.iterator().next());
            }
            WizardDialog dialog =
                new WizardDialog(
                    view.getSite().getShell(),
                    new GenerateReportWizard(project, view.getSite().getWorkbenchWindow().getWorkbench()));
            dialog.create();
            dialog.open();
        } catch (Exception e) {
            CloverPlugin.logError("Unable to launch report wizard", e);
        }
    }

    @Override
    public Menu getMenu(Control control) {
        disposeMenu();
        menu = new Menu(control);
        return buildMenu();
    }

    @Override
    public Menu getMenu(Menu parent) {
        disposeMenu();
        menu = new Menu(parent);
        return buildMenu();
    }

    private Menu buildMenu() {
        MenuItem runItem = new MenuItem(menu, SWT.CASCADE);
        runItem.setText(CloverEclipsePluginMessages.PROJECT_GENERATE_REPORT());
        runItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                generate();
            }
        });
        MenuItem viewItem = new MenuItem(menu, SWT.CASCADE);
        viewItem.setText(CloverEclipsePluginMessages.PROJECT_VIEW_REPORT());

        List<ReportHistoryEntry> reportHistory = CloverPlugin.getInstance().getReportHistory();
        if (reportHistory.size() > 0) {
            Menu viewItems = new Menu(viewItem);
            Collections.reverse(reportHistory);
            for (final ReportHistoryEntry report : reportHistory) {
                MenuItem viewItemsItem = new MenuItem(viewItems, SWT.NONE);
                viewItemsItem.setText(report.toString());
                viewItemsItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        List availableMethods = OpenReportOperation.findFor(report);

                        OpenReportOperation chosenMethod =
                                availableMethods.size() == 1
                                        ? (OpenReportOperation) availableMethods.get(0)
                                        : OpenReportDialog.openOnRevist(
                                        view.getSite().getShell(),
                                        report,
                                        availableMethods);

                        if (chosenMethod != null) {
                            chosenMethod.open(report);
                        }
                    }
                });
            }
            viewItem.setMenu(viewItems);
        } else {
            viewItem.setEnabled(false);
        }
        return menu;
    }

    @Override
    public void dispose() {
        disposeMenu();
    }

    private void disposeMenu() {
        if (menu != null && !menu.isDisposed()) {
            menu.dispose();
        }
    }
}
