package org.openclover.eclipse.core.views.testrunexplorer;

import org.eclipse.ui.IMemento;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.ColumnCollectionSettings;
import org.openclover.eclipse.core.views.ExplorerViewSettings;
import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.FlatTestCasesHierarchyBuilder;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.PackageRootsHierarchyBuilder;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.PackagesHierarchyBuilder;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.TestCaseNodeFactory;

import java.util.Arrays;
import java.util.List;

public class TestRunExplorerViewSettings
    extends ExplorerViewSettings {

    public static final int HIERARCHY_STYLE_START = 0;
    public static final int HIERARCHY_STYLE_PACKAGE_ROOTS = HIERARCHY_STYLE_START;
    public static final int HIERARCHY_STYLE_PACKAGES = 1;
    public static final int HIERARCHY_STYLE_FLAT_TEST_CASES = 2;
    public static final int HIERARCHY_STYLE_MAX = HIERARCHY_STYLE_FLAT_TEST_CASES;

    private TestCaseNodeFactory tcnFactory;
    private ColumnCollectionSettings classesTestedTreeSettings;

    public TestRunExplorerViewSettings(IMemento memento, TestCaseNodeFactory tcnFactory) {
        super(memento);
        classesTestedTreeSettings =
            new ColumnCollectionSettings(
                "ClassesTested",
                memento,
                allClassesTestedTreeColumns(),
                defaultClassesTestedTreeColumns(),
                defaultClassesTestedTreeColumnWidths());
        this.tcnFactory = tcnFactory;
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        classesTestedTreeSettings.saveState(memento);
    }

    @Override
    protected List allTreeColumns() {
        return Arrays.asList(TestRunExplorerView.TESTCASE_BUILTIN_COLUMN_DEFINITIONS);
    }

    @Override
    protected List defaultTreeColumns() {
        return Arrays.asList(TestRunExplorerView.DEFAULT_TESTCASE_BUILTIN_COLUMN_DEFINITIONS);
    }

    @Override
    protected List defaultTreeColumnWidths() {
        return Arrays.asList(200, 100, 50, 75, 200);
    }

    private List defaultClassesTestedTreeColumnWidths() {
        return Arrays.asList(150, 125, 100);
    }

    private List allClassesTestedTreeColumns() {
        return Arrays.asList(TestRunExplorerView.ALL_BUILTIN_CONTRIB_COLUMNS);
    }

    private List defaultClassesTestedTreeColumns() {
        return Arrays.asList(TestRunExplorerView.DEFAULT_BUILTIN_CONTRIB_COLUMNS);
    }

    @Override
    public MetricsScope getMetricsScope() {
        return MetricsScope.TEST_ONLY;
    }

    @Override
    protected NodeHierarchyBuilder[] getNodeBuilders() {
        return new NodeHierarchyBuilder[]{
            new PackageRootsHierarchyBuilder(tcnFactory),
            new PackagesHierarchyBuilder(tcnFactory),
            new FlatTestCasesHierarchyBuilder(tcnFactory),
        };
    }

    @Override
    protected int expandDepthForStyle() {
        switch (hierarchyStyle) {
            case HIERARCHY_STYLE_PACKAGE_ROOTS:
                return 5;
            case HIERARCHY_STYLE_PACKAGES:
                return 4;
            case HIERARCHY_STYLE_FLAT_TEST_CASES:
                return 2;
            default:
                return 2;
        }
    }

    @Override
    protected int getHierarchyStyleMax() {
        return HIERARCHY_STYLE_MAX;
    }

    @Override
    protected int getHierarchyStyleStart() {
        return HIERARCHY_STYLE_START;
    }

    @Override
    protected int defaultHierarchyStyle() {
        return HIERARCHY_STYLE_FLAT_TEST_CASES;
    }

    public ColumnCollectionSettings getClassesTestedTreeSettings() {
        return classesTestedTreeSettings;
    }
}
