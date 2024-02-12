package org.openclover.eclipse.core.views.coverageexplorer;

import org.eclipse.ui.IMemento;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.ExplorerViewSettings;
import org.openclover.eclipse.core.views.coverageexplorer.nodes.FlatPackagesHierarchyBuilder;
import org.openclover.eclipse.core.views.coverageexplorer.nodes.PackageRootsFlatPackagesHierarchyBuilder;
import org.openclover.eclipse.core.views.coverageexplorer.nodes.PackageRootsTreePackagesHierarchyBuilder;
import org.openclover.eclipse.core.views.coverageexplorer.nodes.TreePackagesHierarchyBuilder;
import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;

import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

public class CoverageViewSettings
    extends ExplorerViewSettings {

    public static final int HIERARCHY_STYLE_START = 0;
    public static final int HIERARCHY_STYLE_PACKAGE_ROOTS = HIERARCHY_STYLE_START;
    public static final int HIERARCHY_STYLE_NO_PACKAGE_ROOTS = 1;
    public static final int HIERARCHY_STYLE_MAX = HIERARCHY_STYLE_NO_PACKAGE_ROOTS;
    
    public static final int COVERAGE_MODEL_START = 0;
    public static final int COVERAGE_MODEL_FULL = COVERAGE_MODEL_START;
    public static final int COVERAGE_MODEL_APP_ONLY = 1;
    public static final int COVERAGE_MODEL_TEST_ONLY = 2;
    public static final int COVERAGE_MODEL_MAX = COVERAGE_MODEL_TEST_ONLY;

    public static final String COVERAGE_MODEL = "coverage.model";
    private static final String HIDE_UNAVAILABLE = "hide.unavailable";
    private static final String HIDE_FULLY_COVERED = "hide.fullycovered";
    private static final String SHOW_PACKAGE_TREE = "show.packagetree";

    private int coverageModel;
    private boolean hideUnavailable;
    private boolean hideUnitTests;
    private boolean hideFullyCovered;
    private boolean showPackageTree;

    public CoverageViewSettings() {
        this(null);
    }

    public CoverageViewSettings(IMemento memento) {
        super(memento);
        coverageModel = defaultCoverageModel();
        hideUnavailable = false;
        hideFullyCovered = false;
        hideUnitTests = false;
        showPackageTree = false;
        
        if (memento != null) {
            Integer coverageModel = memento.getInteger(COVERAGE_MODEL);
            if (coverageModel != null) {
                this.coverageModel = coverageModel;
            }

            Integer hideUnavailable = memento.getInteger(HIDE_UNAVAILABLE);
            if (hideUnavailable != null) {
                this.hideUnavailable = hideUnavailable != 0;
            }

            Integer hideFullyCovered = memento.getInteger(HIDE_FULLY_COVERED);
            if (hideUnavailable != null) {
                this.hideFullyCovered = hideFullyCovered != 0;
            }
            Integer showPackageTree = memento.getInteger(SHOW_PACKAGE_TREE);
            if (showPackageTree != null) {
                this.showPackageTree = showPackageTree != 0;
            }
        }
    }

    @Override
    protected List allTreeColumns() {
        return newArrayList(CoverageView.BUILTIN_COLUMN_DEFINITIONS);
    }

    @Override
    protected List defaultTreeColumns() {
        return newArrayList(CoverageView.DEFAULT_BUILTIN_COLUMN_DEFINITIONS);
    }

    @Override
    protected List defaultTreeColumnWidths() {
        return newArrayList(300, MetricsPcCellRenderer.getDefaultWidth(), 100, 100);
    }

    private int defaultCoverageModel() {
        return COVERAGE_MODEL_FULL;
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        memento.putInteger(COVERAGE_MODEL, coverageModel);
        memento.putInteger(HIDE_UNAVAILABLE, hideUnavailable ? 1 : 0);
        memento.putInteger(HIDE_FULLY_COVERED, hideFullyCovered ? 1 : 0);
        memento.putInteger(SHOW_PACKAGE_TREE, showPackageTree ? 1 : 0);
    }

    public int getCoverageModel() {
        return coverageModel;
    }

    public void setCoverageModel(int coverageModel) {
        this.coverageModel = coverageModel;
    }

    public boolean shouldHideUnavailableCoverage() {
        return hideUnavailable;
    }

    public void setShouldHideUnavailableCoverage(boolean shouldHide) {
        hideUnavailable = shouldHide;
    }

    public boolean shouldHideFullyCovered() {
        return hideFullyCovered;
    }

    public void setShouldHideFullyCovered(boolean shouldHide) {
        hideFullyCovered = shouldHide;
    }

    @Override
    public MetricsScope getMetricsScope() {
        switch (coverageModel) {
            case CoverageViewSettings.COVERAGE_MODEL_FULL:
                return MetricsScope.FULL;
            case CoverageViewSettings.COVERAGE_MODEL_APP_ONLY:
                return MetricsScope.APP_ONLY;
            case CoverageViewSettings.COVERAGE_MODEL_TEST_ONLY:
                return MetricsScope.TEST_ONLY;
            default:
                return MetricsScope.DEFAULT;
        }
    }

    @Override
    protected NodeHierarchyBuilder[] getNodeBuilders() {
        return new NodeHierarchyBuilder[] {
            new PackageRootsFlatPackagesHierarchyBuilder(),
            new FlatPackagesHierarchyBuilder(),
            new PackageRootsTreePackagesHierarchyBuilder(),
            new TreePackagesHierarchyBuilder(),
        };
    }

    @Override
    protected int expandDepthForStyle() {
        switch (hierarchyStyle) {
            case HIERARCHY_STYLE_PACKAGE_ROOTS:
                return 3;
            case HIERARCHY_STYLE_NO_PACKAGE_ROOTS:
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
        return HIERARCHY_STYLE_NO_PACKAGE_ROOTS;
    }

    public void showPackageTree(boolean show) {
        showPackageTree = show;
    }

    public boolean showPackageTree() {
        return showPackageTree;
    }

    @Override
    public NodeHierarchyBuilder nodeBuilderForStyle() {
        if (hierarchyStyle >= getHierarchyStyleStart() && hierarchyStyle <= getHierarchyStyleMax()) {
            return getNodeBuilders()[hierarchyStyle + (showPackageTree ? 2 : 0)];
        } else {
            return getNodeBuilders()[defaultHierarchyStyle()];
        }
    }
}
