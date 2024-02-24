package org.openclover.idea.report.cloud;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.PackageFragment;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.reporters.CloudGenerator;
import org.openclover.core.reporters.html.ClassInfoStatsCalculator;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;
import org.openclover.core.reporters.html.HtmlReporter;
import org.openclover.core.reporters.html.RenderCoverageCloudAction;
import org.openclover.core.reporters.html.TabInfo;
import org.openclover.idea.HasMetricsListener;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.actions.Constants;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.coverage.BaseCoverageNodeViewer;
import org.openclover.idea.coverage.CoverageListener;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.coverage.ModelUtil;
import org.openclover.idea.util.ModelScope;
import org.openclover.idea.util.ui.CloverIcons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class CloudEditorController
        implements CoverageListener, HasMetricsListener, ConfigChangeListener, DataProvider {
    private final CoverageManager coverageManager;
    private final CloudVirtualFile cloudVirtualFile;
    private final CloudReportView cloudView;
    private final IdeaCloverConfig config;

    private boolean includeSubpkgs;
    private ModelScope modelScope;

    public CloudEditorController(Project project, CloudVirtualFile cloudVirtualFile, CloudReportView cloudView) {
        this.cloudView = cloudView;
        this.cloudVirtualFile = cloudVirtualFile;
        coverageManager = cloudVirtualFile.getCoverageManager();
        config = ProjectPlugin.getPlugin(project).getConfig();

        includeSubpkgs = config.isCloudReportIncludeSubpkgs();
        modelScope = config.getModelScope();

        config.addConfigChangeListener(this);
        coverageManager.addCoverageListener(this);
        cloudVirtualFile.addHasMetricsListener(this);


    }

    /**
     * Generates class name list where font size is proportional to class avg complexity and font color proportional to class coverage percentage.
     *
     * @param classes   list of classes
     * @return html
     * @see RenderCoverageCloudAction#renderProjectRisks(java.io.File, java.util.List, TabInfo, HtmlReporter.TreeInfo)
     */
    private String generateRiskHtml(List<ClassInfo> classes) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final HtmlRenderingSupportImpl htmlRenderingSupport = new IdeaEditorLinkingHtmlRenderingSupport();

        CloudGenerator cloudGenerator = new CloudGenerator("cloud-idea.vm", htmlRenderingSupport, outputStream);
        try {
            cloudGenerator.createReport(classes,
                    new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                    new ClassInfoStatsCalculator.PcCoveredElementsCalculator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString();
    }

    /**
     * Generates class name list where font size is proportional to total element count and font color proportional to count of uncovered elements
     *
     * @param classes   list of classes
     * @return html
     * @see RenderCoverageCloudAction#renderQuickWins(java.io.File, java.util.List, TabInfo, HtmlReporter.TreeInfo)
     */
    private String generateWinHtml(List<ClassInfo> classes) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final HtmlRenderingSupportImpl htmlRenderingSupport = new IdeaEditorLinkingHtmlRenderingSupport();

        CloudGenerator cloudGenerator = new CloudGenerator("cloud-idea.vm", htmlRenderingSupport, outputStream);
        try {
            cloudGenerator.createReport(classes,
                    new ClassInfoStatsCalculator.ElementCountCalculator(),
                    new ClassInfoStatsCalculator.CoveredElementsCalculator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString();
    }

    private void update() {
        update(coverageManager.getCoverage(), cloudVirtualFile.getSelectedElement());
    }

    @Override
    public void update(final CloverDatabase db) {
        update(db, cloudVirtualFile.getSelectedElement());
    }

    @Override
    public void valueChanged(HasMetrics hasMetrics) {
        update(coverageManager.getCoverage(), hasMetrics);
    }

    private void update(final CloverDatabase db, final HasMetrics selectedElement) {
        cacheElement(db, selectedElement);
        if (db == null) {
            ApplicationManager.getApplication().invokeLater(cloudView::clean);
            return;
        }
        final String packagePrefix = packagePrefix(selectedElement);
        final AggregatingFilter aggregate = new AggregatingFilter(packagePrefix, selectedElement == null || includeSubpkgs);
        final FullProjectInfo projectInfo = ModelUtil.getModel(db, modelScope);

        final List<ClassInfo> allClasses = projectInfo.getClasses(aggregate);
        final String riskContent = generateRiskHtml(allClasses);
        final String winsContent = generateWinHtml(allClasses);
        ApplicationManager.getApplication().invokeLater(() -> {
            setPackagePrefixInfo(packagePrefix);
            cloudView.setRisksHtml(riskContent);
            cloudView.setWinsHtml(winsContent);
            final HasMetrics element = selectedElement != null ? selectedElement : projectInfo;
            cloudView.setSummaryNode(element, new BaseCoverageNodeViewer.TestPassInfo(db.getFullModel().getMetrics()));
        });
    }

    private void setPackagePrefixInfo(String packagePrefix) {
        if (packagePrefix == null) {
            cloudView.setSummaryIcon(CloverIcons.PROJECT);
            cloudView.setSummaryText("Entire project");
        } else {
            cloudView.setSummaryIcon(CloverIcons.PACKAGE_CLOSED);
            cloudView.setSummaryText("Package " + packagePrefix);
        }
    }

    void dispose() {
        coverageManager.removeCoverageListener(this);
        cloudVirtualFile.removeHasMetricsListener(this);
        config.removeConfigChangeListener(this);
    }

    @Nullable
    private String packagePrefix(HasMetrics selectedElement) {
        if (selectedElement instanceof PackageInfo) {
            return selectedElement.getName();
        } else if (selectedElement instanceof PackageFragment) {
            return ((PackageFragment) selectedElement).getQualifiedName();
        } else {
            return null;
        }
    }


    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.CLOUD_REPORT_INCLUDE_SUBPKGS)) {
            includeSubpkgs = (Boolean) evt.getPropertyChange(IdeaCloverConfig.CLOUD_REPORT_INCLUDE_SUBPKGS).getNewValue();
            update();
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.MODEL_SCOPE)) {
            modelScope = (ModelScope) evt.getPropertyChange(IdeaCloverConfig.MODEL_SCOPE).getNewValue();
            update();
        }
    }

    // cache last selected package to enable toggling between package and project views
    private WeakReference<HasMetrics> lastSelectedElement;
    private WeakReference<CloverDatabase> lastSelectedDatabase;

    private void cacheElement(CloverDatabase db, HasMetrics element) {
        if (db == null) {
            // db cleaned - invalidate cache
            lastSelectedDatabase = null;
            lastSelectedElement = null;
        } else {
            if (element != null) {
                lastSelectedDatabase = new WeakReference<>(db);
                lastSelectedElement = new WeakReference<>(element);
            }
        }
    }

    private HasMetrics getCachedElement(CloverDatabase currentDb) {
        return lastSelectedDatabase != null && lastSelectedDatabase.get() == currentDb && lastSelectedElement != null ?
                lastSelectedElement.get() : null;
    }

    @Override
    @Nullable
    public Object getData(@NonNls String dataId) {
        return Constants.SELECTED_HAS_METRICS.getName().equals(dataId) ? getCachedElement(coverageManager.getCoverage()) : null;
    }
}

class MinMaxIntPair {
    int min = Integer.MAX_VALUE;
    int max;

    void update(int val) {
        if (val < min) {
            min = val;
        }
        if (val > max) {
            max = val;
        }
    }

    public int getScale() {
        return min == max ? 1 : max - min;
    }
}

class AggregatingFilter implements HasMetricsFilter {
    final MinMaxIntPair complexity = new MinMaxIntPair();
    final MinMaxIntPair elements = new MinMaxIntPair();
    final MinMaxIntPair uncovered = new MinMaxIntPair();

    float maxAvgComplexity;
    float minAvgComplexity = Float.MAX_VALUE;

    final String packagePrefix;
    final int packagePrefixLen;
    final boolean includeSubpkgs;

    public AggregatingFilter(final String packagePrefix, boolean includeSubpkgs) {
        this.includeSubpkgs = includeSubpkgs;
        this.packagePrefix = packagePrefix == null || packagePrefix.length() == 0 || PackageInfo.DEFAULT_PACKAGE_NAME.equals(packagePrefix) ?
                null : packagePrefix + ".";
        this.packagePrefixLen = this.packagePrefix == null ? 0 : this.packagePrefix.length();
    }

    @Override
    public boolean accept(HasMetrics hm) {
        final String qualifiedName = ((ClassInfo) hm).getQualifiedName();
        if (packagePrefix != null && !qualifiedName.startsWith(packagePrefix)) {
            return false;
        }
        if (!includeSubpkgs && qualifiedName.indexOf('.', packagePrefixLen) != -1) {
            return false;
        }

        final ClassMetrics metrics = (ClassMetrics) hm.getMetrics();
        final int complexity = metrics.getComplexity();
        this.complexity.update(complexity);

        final int numMethods = metrics.getNumMethods();
        if (numMethods > 0) {
            final float avgComplexity = (float) complexity / numMethods;
            if (avgComplexity > maxAvgComplexity) {
                maxAvgComplexity = avgComplexity;
            }
            if (avgComplexity < minAvgComplexity) {
                minAvgComplexity = avgComplexity;
            }
        }

        elements.update(metrics.getNumElements());
        uncovered.update(metrics.getNumUncoveredElements());

        return true;
    }
}