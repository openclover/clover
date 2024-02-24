package org.openclover.core.reporters.json;

import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.registry.entities.BaseClassInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.html.ClassInfoStatsCalculator;
import org.openclover.core.reporters.html.HtmlReportUtil;
import org.openclover.core.reporters.html.StatisticsClassInfoVisitor;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class RenderCloudsJSONAction implements Callable {
    public static final String AGGREGATE_PREFIX = "aggregate-";
    public static final String PROJECT_RISKS_FILE_NAME = "proj-risks.js";
    public static final String PACKAGE_RISKS_FILE_NAME = "pkg-risks.js";
    public static final String QUICK_WINS_FILE_NAME = "quick-wins.js";

    protected final File dir;
    protected final CloverReportConfig cfg;
    protected final VelocityContext ctx;
    protected final boolean aggregate;

    public RenderCloudsJSONAction(VelocityContext ctx, CloverReportConfig cfg, File dir, boolean aggregate) {
        this.cfg = cfg;
        this.dir = dir;
        this.ctx = ctx;
        this.aggregate = aggregate;
    }

    @Override
    public Object call() throws Exception {
        return Void.TYPE;
    }

    protected void apply(File dir, String fileName, String pageTitle, List deepClasses, ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2) throws Exception {
        JSONObject json = new JSONObject();
        applyAxis(deepClasses, axis1, axis2, json);
        ctx.put("json", json);
        ctx.put("callback", cfg.getFormat().getCallback());
        HtmlReportUtil.mergeTemplateToFile(new File(dir, fileName), ctx, "api-json.vm");
    }

    protected void applyAxis(List classes, ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2, JSONObject json) throws JSONException {
        final StatisticsClassInfoVisitor v2 = StatisticsClassInfoVisitor.visit(sort(classes), axis2);
        final StatisticsClassInfoVisitor v1 = StatisticsClassInfoVisitor.visit(v2.getClasses(), axis1);
        json.put("axis",
            new JSONObject()
                .put("x", new JSONObject().put("title", axis1.getName()).put("min", v1.getMin()).put("max", v1.getMax()))
                .put("y", new JSONObject().put("title", axis2.getName()).put("min", v2.getMin()).put("max", v2.getMax())));
        final JSONArray jsonClasses = new JSONArray();
        for (ClassInfo baseClassInfo : v1.getClasses()) {
            final FullClassInfo classInfo = (FullClassInfo) baseClassInfo;
            final String path = classInfo.getContainingFile().getPackagePath();
            jsonClasses.put(
                    new JSONObject()
                            .put("name", classInfo.getName())
                            .put("path", path.substring(0, path.length() - ".java".length()))
                            .put("x", axis1.getScaledValue(classInfo))
                            .put("y", axis2.getScaledValue(classInfo)));
        }
        json.put("classes", jsonClasses);
    }

    protected List sort(List classes) {
        classes.sort(HasMetricsSupport.CMP_LEX);
        return classes;
    }

    public abstract static class ForProjects extends RenderCloudsJSONAction {
        protected final FullProjectInfo project;

        protected ForProjects(FullProjectInfo project, VelocityContext ctx, CloverReportConfig cfg, File dir) {
            super(ctx, cfg, dir, false);
            this.project = project;
        }

        public static class OfTheirRisks extends ForProjects {
            public OfTheirRisks(FullProjectInfo project, VelocityContext ctx, CloverReportConfig cfg, File dir) {
                super(project, ctx, cfg, dir);
            }

            @Override
            public Object call() throws Exception {
                apply(
                    dir, PROJECT_RISKS_FILE_NAME, "Project Risks",
                    project.getClasses(HasMetricsFilter.ACCEPT_ALL),
                    new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                    new ClassInfoStatsCalculator.PcCoveredElementsCalculator());
                return super.call();
            }
        }
        public static class OfTheirQuickWins extends ForProjects {
            public OfTheirQuickWins(FullProjectInfo project, VelocityContext ctx, CloverReportConfig cfg, File dir) {
                super(project, ctx, cfg, dir);
            }

            @Override
            public Object call() throws Exception {
                apply(
                    dir, QUICK_WINS_FILE_NAME, "Quick Wins",
                    project.getClasses(HasMetricsFilter.ACCEPT_ALL),
                    new ClassInfoStatsCalculator.ElementCountCalculator(),
                    new ClassInfoStatsCalculator.CoveredElementsCalculator());
                return super.call();
            }
        }
    }

    public abstract static class ForPackages extends RenderCloudsJSONAction {
        protected final FullPackageInfo pkg;

        protected ForPackages(VelocityContext ctx, FullPackageInfo pkg, CloverReportConfig cfg, File dir, boolean aggregate) {
            super(ctx, cfg, dir, aggregate);
            this.pkg = pkg;
        }

        public static class OfTheirRisks extends ForPackages {
            public OfTheirRisks(VelocityContext ctx, FullPackageInfo pkg, CloverReportConfig cfg, File dir, boolean aggregate) {
                super(ctx, pkg, cfg, dir, aggregate);
            }

            @Override
            public Object call() throws Exception {
                apply(
                    dir, (aggregate ? AGGREGATE_PREFIX : "") + PACKAGE_RISKS_FILE_NAME, "Package Risks",
                    aggregate ? pkg.getClassesIncludingSubPackages() : pkg.getClasses(),
                    new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                    new ClassInfoStatsCalculator.PcCoveredElementsCalculator());
                return super.call();
            }
        }

        public static class OfTheirQuickWins extends ForPackages {
            public OfTheirQuickWins(VelocityContext ctx, FullPackageInfo pkg, CloverReportConfig cfg, File dir, boolean aggregate) {
                super(ctx, pkg, cfg, dir, aggregate);
            }

            @Override
            public Object call() throws Exception {
                apply(
                    dir, (aggregate ? AGGREGATE_PREFIX : "") + QUICK_WINS_FILE_NAME, "Quick Wins",
                    aggregate ? pkg.getAllClassesIncludingSubPackages() : pkg.getClasses(),
                    new ClassInfoStatsCalculator.ElementCountCalculator(),
                    new ClassInfoStatsCalculator.CoveredElementsCalculator());
                return super.call();
            }
        }
    }
}
