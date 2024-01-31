package com.atlassian.clover.reporters.html;

import clover.org.apache.commons.lang3.StringUtils;
import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.BitSetCoverageProvider;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.util.Formatting;
import com.atlassian.clover.util.CloverUtils;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class RenderTestResultAction implements Callable {
    private static final ThreadLocal<FullProjectInfo> REUSABLE_MODEL = new ThreadLocal<>();
    private static final ThreadLocal<FullProjectInfo> CONFIGURABLE_MODEL = new ThreadLocal<>();

    private static final Comparator<HasMetrics> TARGET_CLASS_COMPARATOR = (hasMetrics1, hasMetrics2) ->
            Float.compare(hasMetrics2.getMetrics().getPcCoveredElements(), hasMetrics1.getMetrics().getPcCoveredElements());

    private final HtmlRenderingSupportImpl renderingHelper; // read only
    private final Current reportConfig; // read only
    private final FullProjectInfo fullModel; // read only - put into velocity context for rendering
    private final TestCaseInfo testCaseInfo; // read only
    private final VelocityContext velocity; // write only

    private final CloverDatabase database; // shared but read only
    private final FullProjectInfo readOnlyModel; // gets copied in thread locals

    public RenderTestResultAction(
            TestCaseInfo testCaseInfo,
            HtmlRenderingSupportImpl renderingHelper,
            Current reportConfig,
            FullProjectInfo readOnlyModel,
            VelocityContext velocity,
            FullProjectInfo fullModel,
            CloverDatabase database) {

        this.renderingHelper = renderingHelper;
        this.reportConfig = reportConfig;
        this.readOnlyModel = readOnlyModel;
        this.testCaseInfo = testCaseInfo;
        this.velocity = velocity;
        this.fullModel = fullModel;
        this.database = database;
    }

    @Override
    public Object call() throws Exception {
        //First action to be called per-thread sets up the TLS models
        if (REUSABLE_MODEL.get() == null) {
            REUSABLE_MODEL.set(readOnlyModel.copy());
        }

        if (CONFIGURABLE_MODEL.get() == null) {
            CONFIGURABLE_MODEL.set(readOnlyModel.copy());
        }

        final FullFileInfo finfo = (FullFileInfo) testCaseInfo.getRuntimeType().getContainingFile();
        final StringBuffer outname = renderingHelper.getTestFileName(testCaseInfo);
        final File outfile = CloverUtils.createOutFile(finfo, outname.toString(), reportConfig.getOutFile());

        FullProjectInfo projectInfo = CONFIGURABLE_MODEL.get();

        final CoverageData data = database.getCoverageData();
        projectInfo.setDataProvider(new BitSetCoverageProvider(data.getHitsFor(testCaseInfo), data)); // read only

        List<? extends BaseClassInfo> classes = getCoverageByTest(projectInfo);

        if (reportConfig.isShowUniqueCoverage()) {
            gatherUniquenessVariables(classes);
        } else {
            velocity.put("showUnique", Boolean.FALSE);
        }

        velocity.put("currentPageURL", outname);

        Collections.sort(classes, TARGET_CLASS_COMPARATOR);
        velocity.put("targetClasses", classes);
        velocity.put("test", testCaseInfo);
        velocity.put("topLevel", Boolean.TRUE);
        velocity.put("projectInfo", fullModel);
        velocity.put("hasResults", fullModel.hasTestResults());
        velocity.put("stringUtils", new StringUtils());
        velocity.put("renderUtil", renderingHelper);

        HtmlReportUtil.mergeTemplateToFile(outfile, velocity,
                "test-summary.vm");
        return null;
    }

    private void gatherUniquenessVariables(List<? extends BaseClassInfo> classes) {
        final Map<String, ClassInfo> uniqueCoverageMap = new LinkedHashMap<>();
        float uniqueElementsHit = buildUniqueCoverageMap(testCaseInfo, uniqueCoverageMap);

        int totalElementsHit = 0;
        for (ClassInfo info : classes) {
            totalElementsHit += info.getMetrics().getNumCoveredElements();
        }

        final float pcUniqueElementsHit = (totalElementsHit > 0f) ? (uniqueElementsHit / totalElementsHit) : 0f;
        final String pcUniqueCoverage = Formatting.getPercentStr(pcUniqueElementsHit);

        velocity.put("showUnique", Boolean.TRUE);
        velocity.put("uniqueTargetClasses", uniqueCoverageMap);
        velocity.put("pcUniqueCoverage", pcUniqueCoverage);
    }

    /**
     * Fills the given map with classname[String],ClassInfo of unique coverage
     *
     * @param tci               the test case info to build the unique coverage for
     * @param uniqueCoverageMap the map to store each ClassInfo object, keyed on {@link com.atlassian.clover.registry.entities.FullClassInfo#getQualifiedName()}, (String)
     * @return the number of unique elements that were hit by the tests
     */
    private int buildUniqueCoverageMap(TestCaseInfo tci, Map<String, ClassInfo> uniqueCoverageMap) {
        final FullProjectInfo projectInfo = createUniqueCoverageModel(tci);
        final List<? extends BaseClassInfo> uniqueClassesCovered = getCoverageByTest(projectInfo);
        Collections.sort(uniqueClassesCovered, TARGET_CLASS_COMPARATOR);

        int uniqueElementsHit = 0;
        for (BaseClassInfo info : uniqueClassesCovered) {
            uniqueCoverageMap.put(info.getQualifiedName(), info);
            uniqueElementsHit += info.getMetrics().getNumCoveredElements();
        }
        return uniqueElementsHit;
    }

    private FullProjectInfo createUniqueCoverageModel(TestCaseInfo tci) {
        FullProjectInfo projectInfo = REUSABLE_MODEL.get();
        final CoverageData data = database.getCoverageData();
        projectInfo.setDataProvider(new BitSetCoverageProvider(data.getUniqueHitsFor(tci), data)); // all read only
        return projectInfo;
    }

    private List<? extends BaseClassInfo> getCoverageByTest(FullProjectInfo projectInfo) {
        return projectInfo.getClasses(hasMetrics ->
                ((!((ClassInfo) hasMetrics).isTestClass()) &&
                        (hasMetrics.getMetrics().getNumCoveredElements() > 0)));
    }
}
