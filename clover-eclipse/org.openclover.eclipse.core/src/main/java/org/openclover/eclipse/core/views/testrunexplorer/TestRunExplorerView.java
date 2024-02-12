package org.openclover.eclipse.core.views.testrunexplorer;


import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.openclover.core.util.MetricsFormatUtils;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.BuiltinColumnDefinition;
import org.openclover.eclipse.core.views.CloveredWorkspaceProvider;
import org.openclover.eclipse.core.views.ColumnBuilder;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.ExplorerView;
import org.openclover.eclipse.core.views.ExplorerViewLabelProvider;
import org.openclover.eclipse.core.views.ExplorerViewSettings;
import org.openclover.eclipse.core.views.TreeColumnControlListener;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.CoverageContributionNode;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.TestCaseNode;
import org.openclover.eclipse.core.views.testrunexplorer.widgets.CoverageContributionCellRenderer;
import org.openclover.eclipse.core.views.testrunexplorer.widgets.TestStatusRenderer;
import org.openclover.eclipse.core.views.widgets.ColumnController;
import org.openclover.eclipse.core.views.widgets.ListeningRenderer;
import org.openclover.eclipse.core.views.widgets.SelectionAwareCellRenderer;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;

import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Maps.newLinkedHashMap;


public class TestRunExplorerView extends ExplorerView {
    public static final String ID = "org.openclover.eclipse.core.views.testrunexplorer";
    //Not threadsafe but will only be ever accessed in the UI thread
    private static DateFormat TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    public static final ColumnDefinition TESTCASE_COL_ELEMENT = new BuiltinColumnDefinition(
        "TestCaseElementName",
        0,
        SWT.LEFT,
        CloverEclipsePluginMessages.TEST_COL(),
        CloverEclipsePluginMessages.TEST_ABBREVIATED_COL(),
        CloverEclipsePluginMessages.TESTT_TOOL_TIP()) {
        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            if (element instanceof TestCaseNode
                && settings.getHierarchyStyle() == TestRunExplorerViewSettings.HIERARCHY_STYLE_FLAT_TEST_CASES) {

                IMethod method = ((TestCaseNode) element).getTestMethod();
                return
                    method.getDeclaringType().getElementName()
                    + "." + delegate.getText(element);
            } else {
                return delegate.getText(element);
            }
        }

        @Override
        public Image getImage(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            if (element instanceof TestCaseNode) {
                return ((TestCaseNode)element).getTestCaseIcon();
            } else {
                return delegate.getImage(element);
            }
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
            if (settings.getHierarchyStyle() == TestRunExplorerViewSettings.HIERARCHY_STYLE_FLAT_TEST_CASES) {
                return TESTCASE_COL_ELEMENT_FLAT_NAME_COMPARATOR;
            } else {
                return TESTCASE_COL_ELEMENT_FULL_NAME_COMPARATOR;
            }
        }
    };
    public static final Comparator TESTCASE_COL_ELEMENT_FLAT_NAME_COMPARATOR = (object1, object2) ->
            TestRunExplorerTreeComparator.compareTestCaseName(object1, object2, true);
    public static final Comparator TESTCASE_COL_ELEMENT_FULL_NAME_COMPARATOR = (object1, object2) ->
            TestRunExplorerTreeComparator.compareTestCaseName(object1, object2, false);

    public static final BuiltinColumnDefinition TESTCASE_COL_STARTED = new BuiltinColumnDefinition(
        "TestCaseStarted",
        BuiltinColumnDefinition.ANY_COLUMN,
        SWT.RIGHT,
        CloverEclipsePluginMessages.TEST_STARTED_COL(),
        CloverEclipsePluginMessages.TEST_STARTED_ABBREVIATED_COL(),
        CloverEclipsePluginMessages.TEST_STARTED_COL_TOOL_TIP()) {

        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            if (element instanceof TestCaseNode) {
                return TIME_FORMAT.format(new Date(((TestCaseNode)element).getStartTime()));
            } else {
                return "";
            }
        }

        @Override
        public ListeningRenderer newRenderer(Composite composite, final ExplorerViewSettings viewSettings) {
            return new SelectionAwareCellRenderer(viewSettings.getTreeColumnSettings(), this, composite) {
                @Override
                protected void paint(Event event) {
                    if (forThisColumn(event)) {
                        renderText(
                            getLabel(viewSettings, viewSettings.getMetricsScope(), null, event.item.getData()),
                            event.gc,
                            event,
                            event.display,
                            SWT.RIGHT);
                    }
                }
            };
        }

        @Override
        public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
            return TESTCASE_COL_STARTED_COMPARATOR;
        }
    };
    public static final Comparator TESTCASE_COL_STARTED_COMPARATOR = TestRunExplorerTreeComparator::compareStarted;

    public static final BuiltinColumnDefinition TESTCASE_COL_STATUS = new BuiltinColumnDefinition(
        "TestCaseStatus",
        BuiltinColumnDefinition.ANY_COLUMN,
        SWT.RIGHT,
        CloverEclipsePluginMessages.TEST_STATUS_COL(),
        CloverEclipsePluginMessages.TEST_STATUS_ABBREVIATED_COL(),
        CloverEclipsePluginMessages.TEST_STATUS_COL_TOOL_TIP()) {

        @Override
        public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
            return new TestStatusRenderer((TestRunExplorerViewSettings) settings, this, composite);
        }

        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            return "";
        }

        @Override
        public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
            return TESTCASE_COL_STATUS_COMPARATOR;
        }
    };

    public static final Comparator TESTCASE_COL_STATUS_COMPARATOR = TestRunExplorerTreeComparator::compareStatus;

    public static final BuiltinColumnDefinition TESTCASE_COL_TIME = new BuiltinColumnDefinition(
        "TestCaseTime",
        BuiltinColumnDefinition.ANY_COLUMN,
        SWT.RIGHT,
        CloverEclipsePluginMessages.TEST_TIME_COL(),
        CloverEclipsePluginMessages.TEST_TIME_ABBREVIATED_COL(),
        CloverEclipsePluginMessages.TEST_TIME_COL_TOOL_TIP()) {

        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            if (element instanceof TestCaseNode) {
                return ((TestCaseNode) element).getDurationInSeconds() + "s";
            } else {
                return "";
            }
        }

        @Override
        public ListeningRenderer newRenderer(Composite composite, final ExplorerViewSettings viewSettings) {
            return new SelectionAwareCellRenderer(viewSettings.getTreeColumnSettings(), this, composite) {
                @Override
                protected void paint(Event event) {
                    if (forThisColumn(event)) {
                        renderText(
                            getLabel(viewSettings, viewSettings.getMetricsScope(), null, event.item.getData()),
                            event.gc,
                            event,
                            event.display,
                            SWT.RIGHT);
                    }
                }
            };
        }
        @Override
        public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
            return TESTCASE_COL_TIME_COMPARATOR;
        }
    };
    public static final Comparator TESTCASE_COL_TIME_COMPARATOR = TestRunExplorerTreeComparator::compareTime;

    public static final BuiltinColumnDefinition TESTCASE_COL_MESSAGE = new BuiltinColumnDefinition(
        "TestCaseMessage",
        BuiltinColumnDefinition.ANY_COLUMN,
        SWT.LEFT,
        CloverEclipsePluginMessages.TEST_MESSAGE_COL(),
        CloverEclipsePluginMessages.TEST_MESSAGE_ABBREVIATED_COL(),
        CloverEclipsePluginMessages.TEST_MESSAGE_COL_TOOL_TIP()) {;

        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            if (element instanceof TestCaseNode) {
                TestCaseNode testCaseNode = (TestCaseNode)element;
                if (testCaseNode.getStatus() == TestCaseNode.STATUS_FAIL && testCaseNode.getFailureMessage() != null) {
                    return testCaseNode.getFailureMessage();
                }
            }
            return "";
        }

        @Override
        public boolean displaysSimpleLabel() {
            return false;
        }

        @Override
        public ListeningRenderer newRenderer(Composite composite, final ExplorerViewSettings viewSettings) {
            return new SelectionAwareCellRenderer(viewSettings.getTreeColumnSettings(), this, composite) {
                @Override
                protected void paint(Event event) {
                    if (forThisColumn(event)) {
                        renderText(
                            getLabel(viewSettings, viewSettings.getMetricsScope(), null, event.item.getData()),
                            event.gc,
                            event,
                            event.display,
                            SWT.LEFT);
                    }
                }
            };
        }

        @Override
        public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
            return TESTCASE_COL_MESSAGE_COMPARATOR;
        }
    };
    public static final Comparator TESTCASE_COL_MESSAGE_COMPARATOR = TestRunExplorerTreeComparator::compareMessage;

    public static final ColumnDefinition[] DEFAULT_TESTCASE_BUILTIN_COLUMN_DEFINITIONS = {
        TESTCASE_COL_ELEMENT,
        TESTCASE_COL_STARTED,
        TESTCASE_COL_STATUS,
        TESTCASE_COL_TIME,
        TESTCASE_COL_MESSAGE,
    };

    public static final ColumnDefinition[] TESTCASE_BUILTIN_COLUMN_DEFINITIONS = {
        TESTCASE_COL_ELEMENT,
        TESTCASE_COL_STARTED,
        TESTCASE_COL_STATUS,
        TESTCASE_COL_TIME,
        TESTCASE_COL_MESSAGE,
    };

    public static final ColumnDefinition CONTRIB_COL_CLASS = new BuiltinColumnDefinition(
        "TestContribElementName",
        0,
        SWT.LEFT,
        "Class",
        "Class",
        "The name of the class coverage by the test case") {

        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            if (element instanceof CoverageContributionNode) {
                return ((CoverageContributionNode) element).getElement().getElementName();
            } else {
                return "";
            }
        }

        @Override
        public Image getImage(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            return delegate.getImage(element);
        }

        @Override
        public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
            return new SelectionAwareCellRenderer(((TestRunExplorerViewSettings)settings).getClassesTestedTreeSettings(), this, composite) { };
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
            return CONTRIB_COL_CLASS_COMPARATOR;
        }
    };
    public static final Comparator CONTRIB_COL_CLASS_COMPARATOR = ClassesTestedTreeComparator::compareName;

    public static final ColumnDefinition CONTRIB_COL_CONTRIB = new BuiltinColumnDefinition(
        "TestContribCoverage",
        ColumnDefinition.ANY_COLUMN,
        SWT.LEFT,
        "Contributed Coverage%",
        "Contrib%",
        "Percentage of the class's total code coverage provided by the test case") {
        @Override
        public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
            return
                new CoverageContributionCellRenderer(
                    (Tree)composite,
                    ((TestRunExplorerViewSettings)settings).getClassesTestedTreeSettings(),
                    this);
        }

        @Override
        public boolean displaysSimpleLabel() {
            return false;
        }

        @Override
        public boolean displaysImage() {
            return false;
        }

        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            return null;
        }

        @Override
        public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
            return CONTRIB_COL_CONTRIB_COMPARATOR;
        }
    };
    public static final Comparator CONTRIB_COL_CONTRIB_COMPARATOR = ClassesTestedTreeComparator::compareContribCoverage;

    public static final ColumnDefinition CONTRIB_COL_UNIQUE = new BuiltinColumnDefinition(
        "TestContribUniqueCoverage",
        ColumnDefinition.ANY_COLUMN,
        SWT.RIGHT,
        "Unique Coverage%",
        "Uniq%",
        "Percentage of the class's code coverage provided by the test case and by no other test case") {

        @Override
        public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
            if (element instanceof CoverageContributionNode) {
                return MetricsFormatUtils.formatMetricsPercent(((CoverageContributionNode) element).getUnique());
            } else {
                return "";
            }
        }

        @Override
        public ListeningRenderer newRenderer(Composite composite, final ExplorerViewSettings viewSettings) {
            return new SelectionAwareCellRenderer(((TestRunExplorerViewSettings)viewSettings).getClassesTestedTreeSettings(), this, composite) {
                @Override
                protected void paint(Event event) {
                    if (forThisColumn(event)) {
                        renderText(
                            getLabel(viewSettings, viewSettings.getMetricsScope(), null, event.item.getData()), 
                            event.gc,
                            event,
                            event.display,
                            SWT.RIGHT);
                    }
                }
            };
        }

        @Override
        public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
            return CONTRIB_COL_UNIQUE_COMPARATOR;
        }
    };
    public static final Comparator CONTRIB_COL_UNIQUE_COMPARATOR = ClassesTestedTreeComparator::compareUniqueCoverage;

    public static final ColumnDefinition[] DEFAULT_BUILTIN_CONTRIB_COLUMNS = {
        CONTRIB_COL_CLASS,
        CONTRIB_COL_CONTRIB,
        CONTRIB_COL_UNIQUE
    };

    public static final ColumnDefinition[] ALL_BUILTIN_CONTRIB_COLUMNS = {
        CONTRIB_COL_CLASS,
        CONTRIB_COL_CONTRIB,
        CONTRIB_COL_UNIQUE
    };

    private Tree classesTestedTree;
    private Map<ColumnDefinition, TreeColumn> classesTestedTreeColumns;
    private Map<TreeColumn, TreeColumnControlListener> classesTestedColumnListeners;
    private TreeViewer classesTestedTreeViewer;
    private ClassesTestedTreeProvider classesTestedTreeProvider;
    private ClassesTestedTreeLabelProvider classesTestedTreeLabelProvider;

//    private Table coverageContribTable;
//    private TreeColumn[] coverageContribTreeColumns;
    private TreeViewer coverageContribPaneViewer;
    private Composite explorerTreePane;
    private Map testCaseNodeCache;
    private ColumnController classesTestColumnController = this::updateClassesTestedSorter;

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        testCaseNodeCache = Collections.synchronizedMap(new WeakHashMap());
        settings = new TestRunExplorerViewSettings(memento, (tci, method) -> {
            synchronized (testCaseNodeCache) {
                Integer hashCode = tci.hashCode();
                TestCaseNode tcn = (TestCaseNode) testCaseNodeCache.get(hashCode);
                if (tcn == null) {
                    tcn = new TestCaseNode(method, tci);
                    testCaseNodeCache.put(hashCode, tcn);
                }
                return tcn;
            }
        });
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        settings.saveState(memento);
    }

    @Override
    protected void buildPartControl(Composite parent) {
        super.buildPartControl(parent);

        classesTestedTreeViewer.addDoubleClickListener(new JavaElementDblClickListener(actions[ACTION_OPEN]) {
            @Override
            protected void doEditorOpenAlternative(Object selected) {
            }
        });
    }

    @Override
    protected void buildTree() {
        explorerTreePane = new Composite(mainContent, SWT.NONE);
        explorerTreePane.setLayout(new GridLayout(1, false));

        new Label(explorerTreePane, SWT.NONE).setText("Tests run:");

        super.buildTree();

        tree.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    @Override
    public int getAutoExpandLevel() {
        //Keep this artificially low to avoid Java model walking when first creating the view
        //Test run explorer tree model needs some efficiency before we can auto-expand everything
        return 1;
    }

    @Override
    protected Composite getTreeParent() {
        return explorerTreePane;
    }

    @Override
    protected void buildRightHandSide() {
        Composite classesTestedPane = new Composite(mainContent, SWT.NONE);
        classesTestedPane.setLayout(new GridLayout(1, false));

        new Label(classesTestedPane, SWT.NONE).setText("Coverage Contribution:");

        classesTestedTree = new Tree(classesTestedPane, SWT.NONE);
        classesTestedTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        classesTestedTree.setLinesVisible(true);
        classesTestedTree.setHeaderVisible(true);

        rebuildClassesTestedColumns();

        classesTestedTreeViewer = new TreeViewer(classesTestedTree);
        classesTestedTreeProvider = new ClassesTestedTreeProvider(classesTestedTreeViewer);
        classesTestedTreeViewer.setLabelProvider(new ClassesTestedTreeLabelProvider(getSettings()));
        classesTestedTreeViewer.setContentProvider(classesTestedTreeProvider);
        classesTestedTreeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
//        classesTestedTable.setMenu(newContextMenuManager().createContextMenu(classesTestedTable));

        updateClassesTestedSorter();

        classesTestedTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent event) {
                if (event.data instanceof CoverageContributionNode) {
                    actions[ACTION_OPEN].run(new StructuredSelection());
                }
            }
        });

        treeViewer.addSelectionChangedListener(classesTestedTreeProvider);

//        Composite contribPane = new Composite(mainContent, SWT.NONE);
//        contribPane.setLayout(new GridLayout(1, false));
//
//        new Label(contribPane, SWT.NONE).setText("Test contributions:");
//
//        coverageContribTable = new Table(contribPane, SWT.NONE);
//        coverageContribTable.setLayoutData(new GridData(GridData.FILL_BOTH));
//        coverageContribTable.setLinesVisible(true);
//        coverageContribTable.setHeaderVisible(true);
//        coverageContribTreeColumns = new TreeColumn[1];
//        coverageContribTreeColumns[0] = new TreeColumn(coverageContribTable, SWT.LEFT);
//        coverageContribTreeColumns[0].setText("Class");
//        coverageContribTreeColumns[0].setToolTipText("TODO");
//        coverageContribTreeColumns[0].setWidth(100);
//
//        classesTestedTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
//            public void selectionChanged(SelectionChangedEvent event) {
//
//            }
//        });

        updateMainContentSashOrientation(calcViewOrientation());
    }

    private void rebuildClassesTestedColumns() {
        if (classesTestedTreeColumns != null) {
            //Remove all listeners to "column moved" events for adjacent columns
            for (Map.Entry<TreeColumn, TreeColumnControlListener> entry : classesTestedColumnListeners.entrySet()) {
                TreeColumn column = entry.getKey();
                TreeColumnControlListener listener = entry.getValue();
                column.removeControlListener(listener);
                column.removeSelectionListener(listener);
            }
            for (Map.Entry<ColumnDefinition, TreeColumn> entry : columns.entrySet()) {
                unbuildClassesTestedTreeColumn(entry.getKey(), entry.getValue());
            }
        }

        classesTestedColumnListeners = newHashMap();
        classesTestedTreeColumns = newLinkedHashMap();

        Map<ColumnDefinition, Integer> columnToWidths =
                getSettings().getClassesTestedTreeSettings().getVisibleColumnsToWidths();
        for (Map.Entry<ColumnDefinition, Integer> entry : columnToWidths.entrySet()) {
            buildClassesTestedTreeColumn(entry.getKey(), entry.getValue());
        }
        int index = 0;
        for (Map.Entry<ColumnDefinition, TreeColumn> entry : classesTestedTreeColumns.entrySet()) {
            ColumnDefinition columnDef = entry.getKey();
            TreeColumn column = entry.getValue();
            TreeColumnControlListener listener = new TreeColumnControlListener(classesTestColumnController, treeColumnLabeler, classesTestedTreeViewer, getSettings().getClassesTestedTreeSettings(), column, index++, columnDef);
            column.addControlListener(listener);
            column.addSelectionListener(listener);
            columnDef.bindRenderer(classesTestedTree, getSettings());
        }
    }

    private void buildClassesTestedTreeColumn(ColumnDefinition columnDefinition, Integer width) {
        TreeColumn column = ColumnBuilder.buildTreeColumn(columnDefinition, classesTestedTree, treeColumnLabeler);
        column.setWidth(width);
        columnDefinition.bindRenderer(classesTestedTree, settings);
        classesTestedTreeColumns.put(columnDefinition, column);
    }

    protected void unbuildClassesTestedTreeColumn(ColumnDefinition columnDefinition, TreeColumn column) {
        columnDefinition.unbindRenderer(column.getParent());
        column.dispose();
    }

    @Override
    protected void updateMainContentSashOrientation(int viewOrientation) {
        super.updateMainContentSashOrientation(viewOrientation);
        switch (viewOrientation) {
            case SWT.VERTICAL:
//                mainContent.setWeights(new int[]{1, 1, 1});
                mainContent.setWeights(new int[]{1, 1});
                break;
            case SWT.HORIZONTAL:
            default:
                mainContent.setWeights(new int[]{4, 3});
//                mainContent.setWeights(new int[]{4, 3, 3});
                break;
        }
    }

    @Override
    protected CloveredWorkspaceProvider newContentProvider() {
        return new TestCaseTreeProvider(this, getSettings());
    }

    @Override
    protected ExplorerViewLabelProvider newLabelProvider() {
        return new TestRunExplorerViewLabelProvider(getSettings());
    }

    @Override
    protected ViewerComparator newTreeViewComparator() {
        return TestRunExplorerTreeComparator.createFor(getSettings());
    }

    private TestRunExplorerViewSettings getSettings() {
        return (TestRunExplorerViewSettings) settings;
    }

    @Override
    protected TreeViewer buildSelectionProvider() {
        return treeViewer;
    }

    protected void doRefreshWorkFor(final IProject[] projects) {
        testCaseNodeCache.clear();

        if (!tree.isDisposed()) {
            TreePath[] treePaths = null;
            ISelection selection = treeViewer.getSelection();

            //Refresh the entire tree - it's too hard working out exactly what
            //needs refreshing
            treeViewer.getControl().setRedraw(false);
            try {
                treeViewer.refresh(null);
            } finally {
                treeViewer.getControl().setRedraw(true);
            }

            //Hack: expand items once updated
//            if (treePaths != null) {
//                getTreeViewer().setExpandedTreePaths(treePaths);
//            }

            treeViewer.setSelection(selection);
        }
    }

    private void updateClassesTestedSorter() {
        classesTestedTreeViewer.getControl().setRedraw(false);
        try {
            classesTestedTree.setSortColumn((TreeColumn) classesTestedTreeColumns.get(getSettings().getClassesTestedTreeSettings().getSortedColumn()));
            classesTestedTree.setSortDirection(getSettings().getClassesTestedTreeSettings().isReverseSort() ? SWT.UP : SWT.DOWN);
            classesTestedTreeViewer.setComparator(ClassesTestedTreeComparator.getFor(getSettings().getClassesTestedTreeSettings()));
        } finally {
            classesTestedTreeViewer.getControl().setRedraw(true);
        }
    }

    public ColumnDefinition[] getBuiltinColumnDefinitions() {
        return TESTCASE_BUILTIN_COLUMN_DEFINITIONS;
    }

    @Override
    protected void buildToolbarStructure() {
        IToolBarManager toolbarMenuManager = getViewSite().getActionBars().getToolBarManager();
        toolbarMenuManager.add(new Separator("project.ops"));
        toolbarMenuManager.add(new Separator());
        toolbarMenuManager.add(new Separator("project.reports"));
        toolbarMenuManager.add(generateReportAction);
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
        menuManager.add(new Separator("view.hierarchy.menu"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("view.columns"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("editor.ops.menu"));
        menuManager.add(new Separator());
        menuManager.add(new Separator("additions"));
    }
}
