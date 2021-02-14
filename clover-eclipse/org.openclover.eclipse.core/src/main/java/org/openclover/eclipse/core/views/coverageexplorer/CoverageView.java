package org.openclover.eclipse.core.views.coverageexplorer;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.views.BuiltinColumnDefinition;
import org.openclover.eclipse.core.views.BuiltinDecimalMetricsColumnDefinition;
import org.openclover.eclipse.core.views.BuiltinIntegralMetricsColumnDefinition;
import org.openclover.eclipse.core.views.BuiltinPcMetricsColumnDefinition;
import org.openclover.eclipse.core.views.CloveredWorkspaceProvider;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.ExplorerView;
import org.openclover.eclipse.core.views.ExplorerViewLabelProvider;
import org.openclover.eclipse.core.views.ExplorerViewSettings;
import org.openclover.eclipse.core.views.ExplorerViewComparator;
import org.openclover.eclipse.core.views.widgets.LinkedProjectRenderer;
import org.openclover.eclipse.core.views.widgets.ListeningRenderer;
import org.openclover.eclipse.core.views.coverageexplorer.widgets.InstallationSettingsDialog;
import org.openclover.eclipse.core.views.coverageexplorer.widgets.ProjectSettingsDialog;
import org.openclover.eclipse.core.views.nodes.PackageFragmentNode;
import com.atlassian.clover.reporters.Columns;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static clover.com.google.common.collect.Lists.newLinkedList;


public class CoverageView extends ExplorerView implements IShowInTarget {
    //Non-standard view ID but this is the same as the Clover 1.x plugin line
    public static final String ID = "org.openclover.eclipse.core.views.CloverView";

    public static final ColumnDefinition COL_ELEMENT =
        new BuiltinColumnDefinition(
            "CoverageElementName",
            0,
            SWT.LEFT,
            CloverEclipsePluginMessages.COVERAGE_ELEMENT_COL(),
            CloverEclipsePluginMessages.COVERAGE_ELEMENT_ABBREVIATED_COL(),
            CloverEclipsePluginMessages.COVERAGE_ELEMENT_COL_TOOL_TIP()) {

            @Override
            public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
                return delegate.getText(element);
            }

            @Override
            public Image getImage(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
                return delegate.getImage(element);
            }

            @Override
            public boolean displaysSimpleLabel() {
                return true;
            }

            @Override
            public boolean displaysImage() {
                return true;
            }

            @Override
            public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
                return COL_ELEMENT_COMPARATOR;
            }

            /** @return a renderer that draws a line under the label to make it look like a clickable link */
            @Override
            public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
                return new LinkedProjectRenderer(composite, settings.getTreeColumnSettings(), this);
            }
        };
    
    public static final Comparator COL_ELEMENT_COMPARATOR = new Comparator() {
        @Override
        public int compare(Object object1, Object object2) {
            if (object1.getClass() == object2.getClass()) {
                return ExplorerViewComparator.compareName(object1, object2);
            } else {
                return ExplorerViewComparator.compareType(object1, object2);
            }
        }
    };
    public static final ColumnDefinition COL_COVERAGE =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.TotalPercentageCovered(),
            "Cov%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_TOTAL_ELEMENTS =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.TotalElements(),
            "El",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_COVERED_ELEMENTS =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.CoveredElements(),
            "Cov El%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_TOTAL_BRANCHES =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.TotalBranches(),
            "Br",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_COVERED_BRANCHES =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.CoveredBranches(),
            "Cov Br%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_TOTAL_STATEMENTS =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.TotalStatements(),
            "St",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_COVERED_STATEMENTS =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.CoveredStatements(),
            "Cov St%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_TOTAL_METHODS =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.TotalMethods(),
            "Me",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_COVERED_METHODS =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.CoveredMethods(),
            "Cov Me%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_TOTAL_CHILDREN =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.TotalChildren(),
            "Kids",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_COMPLEXITY =
        new BuiltinDecimalMetricsColumnDefinition(
            new Columns.Complexity(),
            "Cpx",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_AVG_METH_COMPLEXITY =
        new BuiltinDecimalMetricsColumnDefinition(
            new Columns.AvgMethodComplexity(),
            "Av Me Cpx",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_COMPLEXITY_DENSITY =
        new BuiltinDecimalMetricsColumnDefinition(
            new Columns.ComplexityDensity(),
            "Cpx Dns",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_AVG_CLASSES_PER_FILE =
        new BuiltinDecimalMetricsColumnDefinition(
            new Columns.AvgClassesPerFile(),
            "Av Cl/Fi",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_AVG_METHODS_PER_CLASS =
        new BuiltinDecimalMetricsColumnDefinition(
            new Columns.AvgMethodsPerClass(),
            "Av Me/Cl",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_AVG_STATEMENTS_PER_METHOD =
        new BuiltinDecimalMetricsColumnDefinition(
            new Columns.AvgStatementsPerMethod(),
            "Av St/Me",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_TOTAL_FILES =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.TotalFiles(),
            "Fi",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_TOTAL_CLASSES =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.TotalClasses(),
            "Cl",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_LINE_COUNT =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.LineCount(),
            "LOC",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_NC_LINE_COUNT =
        new BuiltinIntegralMetricsColumnDefinition(
            new Columns.NcLineCount(),
            "NCLOC",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_UNCOVERED_BRANCHES =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.UncoveredBranches(),
            "Ucov Br%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_UNCOVERED_STATEMENTS =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.UncoveredStatements(),
            "Ucov St%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_UNCOVERED_METHODS =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.UncoveredMethods(),
            "Ucov Me%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_UNCOVERED_ELEMENTS =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.UncoveredElements(),
            "Ucov El%",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_SUM =
        new BuiltinDecimalMetricsColumnDefinition(
            new Columns.SUM(),
            "SUM",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_COVERED_CONTRIBUTION =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.PercentageCoveredContribution(),
            "%Cov Ctrb",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);
    public static final ColumnDefinition COL_UNCOVERED_CONTRIBUTION =
        new BuiltinPcMetricsColumnDefinition(
            new Columns.PercentageUncoveredContribution(),
            "%Ucov Ctrb",
            ColumnDefinition.ANY_COLUMN,
            SWT.RIGHT);

    public static final ColumnDefinition[] BUILTIN_COLUMN_DEFINITIONS = {
        COL_ELEMENT,
        COL_COVERAGE,
        COL_TOTAL_ELEMENTS,
        COL_COVERED_ELEMENTS,
        COL_TOTAL_BRANCHES,
        COL_COVERED_BRANCHES,
        COL_TOTAL_STATEMENTS,
        COL_COVERED_STATEMENTS,
        COL_TOTAL_METHODS,
        COL_COVERED_METHODS,
        COL_TOTAL_CHILDREN,
        COL_COMPLEXITY,
        COL_AVG_METH_COMPLEXITY,
        COL_COMPLEXITY_DENSITY,
        COL_AVG_CLASSES_PER_FILE,
        COL_AVG_METHODS_PER_CLASS,
        COL_AVG_STATEMENTS_PER_METHOD,
        COL_TOTAL_FILES,
        COL_TOTAL_CLASSES,
        COL_LINE_COUNT,
        COL_NC_LINE_COUNT,
        COL_UNCOVERED_BRANCHES,
        COL_UNCOVERED_STATEMENTS,
        COL_UNCOVERED_METHODS,
        COL_UNCOVERED_ELEMENTS,
        COL_SUM,
        COL_COVERED_CONTRIBUTION,
        COL_UNCOVERED_CONTRIBUTION
    };

    public static final ColumnDefinition[] DEFAULT_BUILTIN_COLUMN_DEFINITIONS = {
        COL_ELEMENT,
        COL_COVERAGE,
        COL_AVG_METH_COMPLEXITY,
        COL_COMPLEXITY,
    };

    private static final int POPUP_OFFSET = 10;

    public static final int ACTION_OPEN = 0;
    public static final int ACTION_MAX = 1;

    private CoverageViewMetricsPane selectionPane;
    private Composite explorerTreePane;
    private Combo modelSelectionCombo;
    private Link settingsLink;

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        settings = new CoverageViewSettings(memento);
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        settings.saveState(memento);
    }

    @Override
    protected void buildRightHandSide() {
        selectionPane = new CoverageViewMetricsPane(mainContent, SWT.NONE, getSettings(), treeViewer);
        updateMainContentSashOrientation(calcViewOrientation());
    }

    @Override
    protected void updateMainContentSashOrientation(int viewOrientation) {
        super.updateMainContentSashOrientation(viewOrientation);
        switch(viewOrientation) {
            case SWT.VERTICAL :
                mainContent.setWeights(new int[] {7, 3});
                break;
            case SWT.HORIZONTAL :
            default :
                mainContent.setWeights(new int[] {2, 1});
                break;
        }
    }

    @Override
    protected void buildTree() {
        explorerTreePane = new Composite(mainContent, SWT.NONE);
        explorerTreePane.setLayout(new GridLayout(3, false));

        new Label(explorerTreePane, SWT.NONE).setText("Show:");

        modelSelectionCombo = new Combo(explorerTreePane, SWT.READ_ONLY);
        modelSelectionCombo.add("All classes");
        modelSelectionCombo.add("Application classes");
        modelSelectionCombo.add("Test classes");
        modelSelectionCombo.select(getSettings().getCoverageModel());

        modelSelectionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                setCoverageModel(modelSelectionCombo.getSelectionIndex());
            }
        });

        settingsLink = new Link(explorerTreePane, SWT.NONE);
        settingsLink.setText("<a>Settings</a>");
        settingsLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        settingsLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                new InstallationSettingsDialog(
                    getSite().getShell(),
                    settingsLink.toDisplay(settingsLink.getBounds().width + POPUP_OFFSET, 0)).open();
            }
        });

        super.buildTree();

        tree.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent event) {
                final Point mousePoint = new Point(event.x, event.y);
                final TreeItem item = tree.getItem(mousePoint);
                if (item != null) {
                    if (item.getData() instanceof IProject
                        && item.getBounds(0).contains(mousePoint)
                        && textBoundsFor(item, 0).contains(mousePoint)) {

                        tree.setCursor(tree.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                        return;
                    }
                }
                tree.setCursor(null);
            }
        });
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));
        SwtUtils.setHorizontalSpan(tree, 3);

        tree.addListener(SWT.MouseDown, new Listener() {
            @Override
            public void handleEvent (Event event) {
                if (event.button == 1) {
                    final Point point = new Point(event.x, event.y);
                    final TreeItem item = tree.getItem (point);
                    final CloverProject cloverProject = getCloverProjectElementAtSelected(point, item);
                    if (cloverProject != null) {
                        Rectangle textBounds = textBoundsFor(item, 0);
                        if (textBounds.contains(point)) {
                            showProjectSettingsPopup(cloverProject, textBounds);
                        }
                    }
                }
            }
        });
    }

    private Rectangle textBoundsFor(TreeItem item, int columnIndex) {
        final GC gc = new GC(tree);
        try {
            final Point textSize;
            try {
                gc.setFont(item.getFont(columnIndex));
                textSize = gc.textExtent(item.getText(columnIndex));
            } finally {
                gc.dispose();
            }
            final Rectangle imageBounds = item.getImageBounds(columnIndex);
            final Point textTopLeft = new Point(imageBounds.x + imageBounds.width + 5, imageBounds.y);
            return new Rectangle(textTopLeft.x, textTopLeft.y, textSize.x, textSize.y);
        } finally {
            gc.dispose();
        }
    }

    private CloverProject getCloverProjectElementAtSelected(Point point, TreeItem item) {
        if (item != null
            && (item.getData() instanceof IProject)
            && item.getBounds(0).contains(point)) {
            try {
                return CloverProject.getFor((IProject)item.getData());
            } catch (CoreException e) {
                //Ignore
            }
        }
        return null;
    }

    private void showProjectSettingsPopup(CloverProject project, final Rectangle textBounds) {
        new ProjectSettingsDialog(
            getSite().getShell(),
            project,
            tree.toDisplay(textBounds.x + textBounds.width + POPUP_OFFSET, textBounds.y)).open();
    }

    @Override
    protected Composite getTreeParent() {
        return explorerTreePane;
    }

    @Override
    protected CloveredWorkspaceProvider newContentProvider() {
        return new CoverageProvider(this, getSettings());
    }

    @Override
    protected ExplorerViewLabelProvider newLabelProvider() {
        return new CoverageViewLabelProvider(getSettings());
    }

    @Override
    protected ViewerComparator newTreeViewComparator() {
        return CoverageViewTreeComparator.createFor(getSettings());
    }

    protected CoverageViewSettings getSettings() {
        return (CoverageViewSettings) settings;
    }

    private CoverageProvider getProvider() {
        return (CoverageProvider) treeContentProvider;
    }

    public int getCoverageModel() {
        return getSettings().getCoverageModel();    
    }

    public void setCoverageModel(int model) {
        modelSelectionCombo.select(model);
        getSettings().setCoverageModel(model);
        refresh();
    }

    public void setShouldHideUnavailableCoverage(boolean shouldHide) {
        getSettings().setShouldHideUnavailableCoverage(shouldHide);
        refresh();
    }

    public void setShouldHideFullyCovered(boolean shouldHide) {
        getSettings().setShouldHideFullyCovered(shouldHide);
        refresh();
    }

    public boolean shouldHideUnavailableCoverage() {
        return getSettings().shouldHideUnavailableCoverage();
    }

    public boolean shouldHideFullyCovered() {
        return getSettings().shouldHideFullyCovered();
    }

    @Override
    protected TreeViewer buildSelectionProvider() {
        return treeViewer;
    }

    public ColumnDefinition[] getBuiltinColumnDefinitions() {
        return BUILTIN_COLUMN_DEFINITIONS;
    }

    public ColumnDefinition[] getDefaultBuiltinColumnDefinitions() {
        return DEFAULT_BUILTIN_COLUMN_DEFINITIONS;
    }

    @Override
    protected void buildToolbarStructure() {
        IToolBarManager toolbarMenuManager = getViewSite().getActionBars().getToolBarManager();
        toolbarMenuManager.add(new Separator("project.ops"));
        toolbarMenuManager.add(new Separator());
        toolbarMenuManager.add(new Separator("project.reports"));
        toolbarMenuManager.add(generateReportAction);
        toolbarMenuManager.add(new Separator());
        toolbarMenuManager.add(new Separator("view.declutter"));
        toolbarMenuManager.add(new Separator());
        toolbarMenuManager.add(new Separator("view.hierarchy"));
        toolbarMenuManager.add(new Separator());
        toolbarMenuManager.add(new Separator("view.columns"));
        toolbarMenuManager.add(new Separator());
        toolbarMenuManager.add(new Separator("editor.ops"));
        toolbarMenuManager.add(new Separator());
        toolbarMenuManager.add(new Separator("additions"));
    }

    @Override
    protected void buildMenubarStructure() {
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
        menuManager.add(new Separator("project.ops"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("project.reports"));
        menuManager.add(generateReportAction);
        menuManager.add(new Separator());
        menuManager.add(new Separator("view.coverage.ops.menu"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("view.workingset"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("view.declutter"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("view.hierarchy.menu"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("view.columns"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("editor.ops.menu"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("additions"));
    }

    @Override
    public boolean show(ShowInContext showInContext) {
        ISelection selection = showInContext.getSelection();
        if (selection != null
            && selection instanceof IStructuredSelection) {
            return selectInTree(((IStructuredSelection)selection).getFirstElement());
        } else {
            return selectInTree(showInContext.getInput());
        }
    }

    private boolean selectInTree(Object input) {
        if (input instanceof IAdaptable) {
            IJavaElement javaElement = (IJavaElement)((IAdaptable)input).getAdapter(IJavaElement.class);
            if (javaElement != null) {
                selectInTree(javaElement);
                return true;
            }
        }
        return false;
    }

    /** Best-effort element selection */
    private void selectInTree(IJavaElement javaElement) {
        //Find path to selection
        List path = newLinkedList();
        while (javaElement != null && !(javaElement instanceof IJavaProject)) {
            Object treeRepresentation = getTreeRepresentationFor(javaElement);
            if (treeRepresentation != null) {
                path.add(treeRepresentation);
            }
            javaElement = javaElement.getParent();
        }
        //Our tree is rooted at IProjects, not IJavaProjects
        if (javaElement instanceof IJavaProject) {
            path.add(javaElement.getJavaProject().getProject());
        }

        //Root to branch
        Collections.reverse(path);

        //We're flying blind here, some of these nodes may not exist
        //as the they may represent the path to a selection that is more
        //grainular than we show in the tree
        for (Iterator iterator = path.iterator(); iterator.hasNext();) {
            Object node = iterator.next();
            getTreeViewer().reveal(node);
            getTreeViewer().expandToLevel(node, 1);
            //If expansion works, it must exist
            if (getTreeViewer().getExpandedState(node) || !iterator.hasNext()) {
                getTreeViewer().setSelection(new StructuredSelection(node), true);
            }
        }
    }

    private Object getTreeRepresentationFor(Object node) {
        //TODO: push this into provider
        if (node instanceof IProject || node instanceof IPackageFragmentRoot || node instanceof ICompilationUnit || node instanceof IType || node instanceof IMethod) {
            return node;
        } else if (node instanceof IPackageFragment && getSettings().getHierarchyStyle() == CoverageViewSettings.HIERARCHY_STYLE_NO_PACKAGE_ROOTS) {
            //Find out the MultiPackageFragmentNodes for the project (IProject as our tree is rooted by those rather than IJavaProject)
            Object[] multiPackageFragments = ((ITreeContentProvider)getTreeViewer().getContentProvider()).getChildren(((IPackageFragment)node).getJavaProject().getProject());
            PackageFragmentNode selectedMultiPackFrag = null;
            for (Object multiPackageFragment : multiPackageFragments) {
                if (multiPackageFragment instanceof PackageFragmentNode) {
                    PackageFragmentNode multPackFrag = (PackageFragmentNode) multiPackageFragment;
                    if (multPackFrag.getPackageFragments().contains(node)) {
                        selectedMultiPackFrag = multPackFrag;
                        break;
                    }
                }
            }
            return selectedMultiPackFrag;
        } else {
            return null;
        }
    }

    public void showPackageTree(boolean show) {
        getSettings().showPackageTree(show);
        treeContentProvider.setNodeBuilder(settings.nodeBuilderForStyle());
        refresh(false);
    }

    public boolean showPackageTree() {
        return getSettings().showPackageTree();
    }
}
