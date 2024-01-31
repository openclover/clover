package org.openclover.eclipse.core.views;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.views.actions.GenerateReportAction;
import org.openclover.eclipse.core.views.actions.OpenJavaEditorAction;
import org.openclover.eclipse.core.views.widgets.ColumnController;
import org.openclover.eclipse.core.views.widgets.ViewAlertContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.actions.NewWizardsActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.GenerateBuildPathActionGroup;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.BuildActionGroup;
import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.ImportActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.NavigateActionGroup;
import org.eclipse.jdt.ui.actions.ProjectActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openclover.util.Maps.newHashMap;
import static org.openclover.util.Maps.newLinkedHashMap;

public abstract class ExplorerView extends CloverViewPart {
    public static final int ACTION_OPEN = 0;
    public static final int ACTION_MAX = 1;
    public static final IProject[] ENTIRE_WORKSPACE = new IProject[] {null};

    /**
     * Workspace tree
     */
    protected Tree tree;
    /**
     * Workspace tree viewer
     */
    protected TreeViewer treeViewer;
    /**
     * Workspace tree columns
     */
    protected Map<ColumnDefinition, TreeColumn> columns;

    /**
     * Adapts column labels as columns are squeezed or stretched
     */
    protected TreeColumnLabeler treeColumnLabeler = new TreeColumnLabeler();
    /**
     * Persisted settings for view
     */
    protected ExplorerViewSettings settings;
    /**
     * Provider of workspace elements for rendering in tree
     */
    protected CloveredWorkspaceProvider treeContentProvider;
    /**
     * Actions used to open source files etc
     */
    protected SelectionDispatchAction[] actions;
    /**
     * Groups of the above actions
     */
    protected ActionGroup[] actionGroups;
    /**
     * Action to generate HTML/XML/PDF reports
     */
    protected GenerateReportAction generateReportAction;

    private ExplorerView.ActivationPartListener thisPartActivationListener;

    protected ColumnController treeColumnController = this::buildTreeSorter;
    private TreeColumnManager treeColumnManager = new TreeColumnManager();
    private Map<TreeColumn, TreeColumnControlListener> columnListeners = newHashMap();

    /**
     * Creates controls for view
     *
     * @param parent parent control of view
     */
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        buildPartControl(parent);
        parent.pack();
        thisPartActivationListener = new ActivationPartListener();
        getSite().getWorkbenchWindow().getPartService().addPartListener(thisPartActivationListener);
    }

    @Override
    public void dispose() {
        if (generateReportAction != null) {
            CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(generateReportAction);
            treeViewer.removeSelectionChangedListener(generateReportAction);
            generateReportAction.dispose();
        }
        if (thisPartActivationListener != null) {
            getSite().getWorkbenchWindow().getPartService().removePartListener(thisPartActivationListener);
        }
        if (alertContainer != null) {
            CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(alertContainer);
        }
        super.dispose();
    }

    public Tree getTree() {
        return tree;
    }

    public TreeViewer getTreeViewer() {
        return treeViewer;
    }

    @Override
    public void setFocus() {
        tree.setFocus();
    }

    public void setHierarchyStyle(int style) {
        settings.setHierarchyStyle(style);
        treeContentProvider.setNodeBuilder(settings.nodeBuilderForStyle());
        refresh(false);
    }

    public int getHierarchyStyle() {
        return settings.getHierarchyStyle();
    }

    public void refresh(boolean preserveExpansion) {
        refresh(preserveExpansion, ENTIRE_WORKSPACE);
    }
    public void refresh() {
        refresh(true, ENTIRE_WORKSPACE);
    }

    public ColumnManager getTreeColumnManager() {
        return treeColumnManager;
    }

    protected void buildTree() {
        tree = new Tree(getTreeParent(), SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.LEFT);
        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);
    }

    protected Composite getTreeParent() {
        return mainContent;
    }

    protected void buildTreeViewer() {
        treeContentProvider = newContentProvider();
        ITableLabelProvider labelprovider = newLabelProvider();

        treeViewer = new TreeViewer(tree);
        treeViewer.setContentProvider(treeContentProvider);
        treeViewer.setAutoExpandLevel(getAutoExpandLevel());
        treeViewer.setInput(ResourcesPlugin.getWorkspace());
        treeViewer.setLabelProvider(labelprovider);
        treeViewer.addSelectionChangedListener(event -> {
            for (SelectionDispatchAction action : actions) {
                action.selectionChanged((IStructuredSelection) event.getSelection());
                for (ActionGroup actionGroup : actionGroups) {
                    actionGroup.setContext(
                            new ActionContext(event.getSelection()));
                }
            }
        });
    }

    private void buildTreeColumns() {
        treeViewer.getControl().setRedraw(false);
        try {
            rebuildTreeColumns();
            tree.setSortColumn(columns.get(settings.getTreeColumnSettings().getSortedColumn()));
            tree.setSortDirection(settings.getTreeColumnSettings().isReverseSort() ? SWT.DOWN : SWT.UP);
        } finally {
            treeViewer.getControl().setRedraw(true);
        }
    }

    protected void buildTreeSorter() {
        treeViewer.getControl().setRedraw(false);
        try {
            tree.setSortColumn(columns.get(settings.getTreeColumnSettings().getSortedColumn()));
            tree.setSortDirection(settings.getTreeColumnSettings().isReverseSort() ? SWT.UP : SWT.DOWN);
            final TreePath[] expanded = treeViewer.getExpandedTreePaths();
            treeViewer.setComparator(newTreeViewComparator());
            treeViewer.setExpandedTreePaths(expanded);
        } finally {
            treeViewer.getControl().setRedraw(true);
        }
    }

    protected void wireGlobalActions() {
        actionGroups = new ActionGroup[]{
            new NewWizardsActionGroup(this.getSite()),
            new NavigateActionGroup(this),
            new CCPActionGroup(this),
            new GenerateBuildPathActionGroup(this),
            new GenerateActionGroup(this),
            new RefactorActionGroup(this),
            new ImportActionGroup(this),
            new BuildActionGroup(this),
            new JavaSearchActionGroup(this),
            new ProjectActionGroup(this),
        };

        actions = new SelectionDispatchAction[ACTION_MAX];
        actions[ACTION_OPEN] = new OpenJavaEditorAction(getSite());
        getViewSite().getActionBars().setGlobalActionHandler(JdtActionConstants.OPEN, actions[ACTION_OPEN]);

        generateReportAction = new GenerateReportAction(this);
        CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(generateReportAction);
        treeViewer.addSelectionChangedListener(generateReportAction);
    }

    protected void buildPartControl(Composite parent) {
        alertContainer = new ViewAlertContainer(parent);
        mainContent = new SashForm(alertContainer, calcViewOrientation());
        mainContent.setLayoutData(new GridData(GridData.FILL_BOTH));
        alertContainer.setContent(mainContent);

        //Beware! Ordering here is very important to avoid NPEs and cyclical dependencies!
        buildTree();
        buildTreeViewer();
        buildTreeColumns();
        buildTreeSorter();
        buildRightHandSide();

        getSite().setSelectionProvider(buildSelectionProvider());

        wireGlobalActions();
        wireToolbarActions();
        wireContextMenu();

        treeViewer.addDoubleClickListener(new JavaElementDblClickListener(actions[ACTION_OPEN]) {
            @Override
            protected void doEditorOpenAlternative(Object selected) {
                treeViewer.setExpandedState(selected, !treeViewer.getExpandedState(selected));
            }
        });

        alertContainer.updateLinks();
        CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(alertContainer);
    }

    protected abstract TreeViewer buildSelectionProvider();

    protected void wireToolbarActions() {
        buildToolbarStructure();
        buildMenubarStructure();
        for (ActionGroup actionGroup : actionGroups) {
            actionGroup.fillActionBars(getViewSite().getActionBars());
        }
    }

    protected abstract void buildToolbarStructure();
    protected abstract void buildMenubarStructure();

    protected void wireContextMenu() {
        MenuManager manager = newContextMenuManager();
        Menu menu = manager.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(manager, treeViewer);
    }

    protected MenuManager newContextMenuManager() {
        MenuManager manager = new MenuManager("#PopupMenu");
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(this::fillContextMenu);
        return manager;
    }

    public void refresh(final boolean preserveAutoExpansion, final Object[] elements) {
        if (elements.length > 0) {
            Display.getDefault().asyncExec(() ->
                    doRefreshWorkFor(preserveAutoExpansion, elements)
            );
        }
    }

    protected void fillContextMenu(IMenuManager manager) {
        manager.add(new Separator("project.ops"));
        manager.add(new Separator());
        manager.add(new Separator("project.reports"));
        manager.add(generateReportAction);
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_NEW));
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_OPEN));
        manager.add(new Separator());
        manager.add(new Separator("group.edit"));
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_GOTO));
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_BUILD));
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
        manager.add(new Separator());
        manager.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));

        for (ActionGroup actionGroup : actionGroups) {
            actionGroup.setContext(new ActionContext(treeViewer.getSelection()));
            try {
                actionGroup.fillContextMenu(manager);
            } catch (Exception e) {
                CloverPlugin.logError("Failed to fully construct context menu", e);
            }
        }
    }

    @Override
    protected int getAssumedViewOrientationIfUnknowable() {
        return SWT.HORIZONTAL;
    }

    protected void nudgeSelectionListeners() {
        //This causes any toolbar duplicated across views with shared state
        //to refresh.
        //There is a better way to do this (plugin actions referenced from views
        //through parameterisation) but no time to implement
        try {
            treeViewer.setSelection(treeViewer.getSelection());
        } catch (Exception e) {
            CloverPlugin.logError("Error nudging selection listeners", e);
        }
    }

    protected void doRefreshWorkFor(boolean preserveExpandedPaths, final Object[] elements) {
        if (!tree.isDisposed()) {
            try {
                treeViewer.getControl().setRedraw(false);
                final TreePath[] expanded = preserveExpandedPaths ? treeViewer.getExpandedTreePaths() : null;
                for (Object element : elements) {
                    treeViewer.refresh(element);
                }
                if (preserveExpandedPaths) {
                    treeViewer.setExpandedTreePaths(expanded);
                } else {
                    treeViewer.expandToLevel(settings.expandDepthForStyle());
                }
            } finally {
                treeViewer.getControl().setRedraw(true);
            }
            nudgeSelectionListeners();
        }
    }

    protected abstract void buildRightHandSide();

    protected abstract CloveredWorkspaceProvider newContentProvider();

    protected abstract ExplorerViewLabelProvider newLabelProvider();

    protected abstract ViewerComparator newTreeViewComparator();

    private void rebuildTreeColumns() {
        if (columns != null) {
            //Remove all listeners to "column moved" events for adjacent columns
            for (Map.Entry<TreeColumn, TreeColumnControlListener> entry : columnListeners.entrySet()) {
                TreeColumn column = entry.getKey();
                TreeColumnControlListener listener = entry.getValue();
                column.removeControlListener(listener);
                column.removeSelectionListener(listener);
            }
            for (Map.Entry<ColumnDefinition, TreeColumn> entry : columns.entrySet()) {
                teardownTreeColumn(entry.getKey(), entry.getValue());
            }
        }

        columnListeners = newHashMap();
        columns = newLinkedHashMap();

        Map<ColumnDefinition, Integer> columnsToWidths = settings.getTreeColumnSettings().getVisibleColumnsToWidths();
        for (Map.Entry<ColumnDefinition, Integer> entry : columnsToWidths.entrySet()) {
            buildTreeColumn(entry.getKey(), entry.getValue());
        }
        int colPosition = 0;
        for (Map.Entry<ColumnDefinition, TreeColumn> entry : columns.entrySet()) {
            ColumnDefinition columnDef = entry.getKey();
            TreeColumn column = entry.getValue();
            TreeColumnControlListener listener = new TreeColumnControlListener(treeColumnController, treeColumnLabeler, treeViewer, settings.getTreeColumnSettings(), column, colPosition++, columnDef);
            column.addControlListener(listener);
            column.addSelectionListener(listener);
            columnListeners.put(column, listener);
        }
    }

    private void buildTreeColumn(ColumnDefinition columnDef, Integer width) {
        TreeColumn column = ColumnBuilder.buildTreeColumn(columnDef, tree, treeColumnLabeler);
        column.setWidth(width);
        columnDef.bindRenderer(tree, settings);
        columns.put(columnDef, column);
    }

    private void teardownTreeColumn(ColumnDefinition columnDefinition, TreeColumn column) {
        columnDefinition.unbindRenderer(column.getParent());
        column.dispose();
    }

    public int getAutoExpandLevel() {
        //Keep this low to avoid Java model walking when first creating the view
        return 2;
    }

    protected abstract class JavaElementDblClickListener implements IDoubleClickListener {
        private SelectionDispatchAction action;

        protected JavaElementDblClickListener(SelectionDispatchAction action) {
            this.action = action;
        }

        @Override
        public void doubleClick(DoubleClickEvent event) {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            Object selected = selection.getFirstElement();
            if (selected instanceof IAdaptable) {
                IJavaElement selectedJavaElement = (IJavaElement) ((IAdaptable) selected).getAdapter(IJavaElement.class);

                if (shouldOpenEditor(event, selectedJavaElement)) {
                    action.run(new StructuredSelection(selectedJavaElement));
                } else {
                    doEditorOpenAlternative(selected);
                }
            }
        }

        protected abstract void doEditorOpenAlternative(Object selected);

        boolean shouldOpenEditor(DoubleClickEvent event, IJavaElement selectedJavaElement) {
            return !(selectedJavaElement instanceof IJavaProject
                || selectedJavaElement instanceof IPackageFragmentRoot
                || selectedJavaElement instanceof IPackageFragment);
        }
    }

    private class ActivationPartListener implements IPartListener2 {
        @Override
        public void partActivated(IWorkbenchPartReference workbenchPartReference) {
            IWorkbenchPart part = workbenchPartReference.getPart(false);
            if (part == ExplorerView.this) {
                nudgeSelectionListeners();
            }
        }

        @Override
        public void partBroughtToTop(IWorkbenchPartReference workbenchPartReference) {}

        @Override
        public void partClosed(IWorkbenchPartReference workbenchPartReference) {}

        @Override
        public void partDeactivated(IWorkbenchPartReference workbenchPartReference) {}

        @Override
        public void partOpened(IWorkbenchPartReference workbenchPartReference) {}

        @Override
        public void partHidden(IWorkbenchPartReference workbenchPartReference) {}

        @Override
        public void partVisible(IWorkbenchPartReference workbenchPartReference) {}

        @Override
        public void partInputChanged(IWorkbenchPartReference workbenchPartReference) {}
    }

    public class TreeColumnManager extends ColumnManager {
        @Override
        public ColumnDefinition[] getAllColumns() {
            Set allColumns = settings.getTreeColumnSettings().getAllColumns();
            return (ColumnDefinition[])allColumns.toArray(new ColumnDefinition[allColumns.size()]);
        }

        @Override
        public ColumnDefinition[] getVisibleColumns() {
            List visibleColumns = settings.getTreeColumnSettings().getVisibleColumns();
            return (ColumnDefinition[])visibleColumns.toArray(new ColumnDefinition[visibleColumns.size()]);
        }

        @Override
        public void update(final CustomColumnDefinition[] custom, final ColumnDefinition[] visible) {
            Display.getDefault().syncExec(() -> {
                settings.getTreeColumnSettings().update(custom, visible);
                rebuildTreeColumns();

                //We need to refresh the tree after columns are moved or elase
                //values start displaying in the wrong columns
                treeViewer.refresh();
            });
        }
    }

}
