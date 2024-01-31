package com.atlassian.clover.reporters.util;

import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.util.Formatting;
import com.atlassian.clover.cfg.Percentage;
import com.atlassian.clover.model.CoverageDataPoint;
import com.atlassian.clover.model.XmlConverter;
import com.atlassian.clover.registry.entities.BasePackageInfo;
import com.atlassian.clover.registry.entities.BaseProjectInfo;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Maps.newTreeMap;

/**
 * Helper class that provides utility methods to support
 * the generation of historical reports
 */
public class HistoricalSupport {

    private static Column DEFAULT_COLUMN = new Columns.TotalPercentageCovered();

    public static class HasMetricsWrapper implements HasMetrics {
        private HasMetrics hm;
        public File dataFile;

        public HasMetricsWrapper(HasMetrics hm, File sourceFile) {
            this.hm = hm;
            this.dataFile = sourceFile;
        }

        @Override
        public String getName() {
            return hm.getName();
        }

        @Override
        public BlockMetrics getMetrics() {
            return hm.getMetrics();
        }

        @Override
        public BlockMetrics getRawMetrics() {
            return hm.getRawMetrics();
        }

        public File getDataFile() {
            return dataFile;
        }

        @Override
        public void setMetrics(BlockMetrics metrics) {
            hm.setMetrics(metrics);
        }

    }

    static SortedMap<Long, HasMetrics> getPackageMetricsForRange(String pkgStr, File[] files, long from, long to) {
        List<CoverageDataPoint> modelList = HistoricalSupport.getModelsForRange(files, from, to, XmlConverter.PACKAGE_LEVEL);
        SortedMap<Long, HasMetrics> result = newTreeMap();

        for (CoverageDataPoint model : modelList) {
            BaseProjectInfo project = model.getProject();
            BasePackageInfo pkg = project.getNamedPackage(pkgStr);
            if (pkg != null) {
                result.put(project.getVersion(), new HasMetricsWrapper(pkg, model.getDataFile()));
            } else {
                Logger.getInstance().warn("Package " + pkgStr + " not found in historical data at " +
                        Formatting.formatDate(new Date(project.getVersion())));
            }
        }
        return result;
    }

    public static SortedMap<Long, HasMetrics> getAllProjectMetrics(File[] files) throws IOException {
        return getProjectMetricsForRange(files, 0, Long.MAX_VALUE);
    }

    static SortedMap<Long, HasMetrics> getProjectMetricsForRange(File[] files, long from, long to) {
        final List<CoverageDataPoint> modelList =
                HistoricalSupport.getModelsForRange(files, from, to, XmlConverter.PROJECT_LEVEL);
        final SortedMap<Long, HasMetrics> result = newTreeMap();
        for (final CoverageDataPoint model : modelList) {
            final BaseProjectInfo project = model.getProject();
            result.put(project.getVersion(), new HasMetricsWrapper(project, model.getDataFile()));
        }
        return result;
    }

    /** get full, class-level model for the given aggregate metrics object */
    public static HasMetrics getFullMetrics(HasMetricsWrapper wrapper, String pkg) throws IOException, CloverException {
        CoverageDataPoint model = XmlConverter.getFromXmlFile(wrapper.getDataFile(), XmlConverter.CLASS_LEVEL);
        if (pkg == null) {
            return model.getProject();
        }
        else {
            return model.getProject().getNamedPackage(pkg);
        }
    }


    /**
     * Returns a list of CoverageDataPoints for a given array of history point files.
     * @param files an array of clover history point files
     * @param from the timestamp to start from
     * @param to only include coverage results up to this
     * @param level one of XmlConverter.*_LEVEL
     * @return a list of CoverageDataPoint objects
     */
    private static List<CoverageDataPoint> getModelsForRange(File[] files, long from, long to, int level) {
        final List<CoverageDataPoint> models = newLinkedList();
        for (final File file : files) {
            CoverageDataPoint model;
            try {
                model = XmlConverter.getFromXmlFile(file, level);
            } catch (Exception e) {
                Logger.getInstance().error(e.getClass().getName() + " occured processing file " + file, e);
                Logger.getInstance().error("Error processing file " + file + ", skipped.");
                continue;
            }
            if (model.getProject() == null) {
                Logger.getInstance().warn("File '" + file.getAbsolutePath() +
                        "' does not contain a valid history point. Ignoring.");
                continue;
            }
            final long ts = model.getProject().getVersion();
            if (ts >= from && ts <= to) {
                models.add(model);
            } else {
                Logger.getInstance().verbose("Snapshot in file " + file + " was outside the specified range.");
            }

        }
        Logger.getInstance().info("Read "+models.size()+" history point"+ ( models.size() == 1 ? "" : "s") +".");
        models.sort(CoverageDataPoint.CHRONOLOGICAL_CMP);
        return models;
    }


    public static List<MetricsDiffSummary> getClassesMetricsDifference(HasMetrics then, HasMetrics now, Percentage threshold, boolean onlyDiffs) throws CloverException {
        return getClassesMetricsDifference(then, now, threshold, DEFAULT_COLUMN, onlyDiffs);
    }

    /**
     * Returns a list of all movers
     *
     * @param column Controls which column of the metrics to use, also an "output parameter" to keep the maximum value of this column
     * @throws CloverException Throws CloverException when a user defines an invalid expression column
     */
    public static List<MetricsDiffSummary> getClassesMetricsDifference(HasMetrics then, HasMetrics now, Percentage threshold, Column column, boolean onlyDiffs) throws CloverException {
        if (then instanceof BasePackageInfo) {
            return getPackageClassesMetricsDiff((BasePackageInfo)then, (BasePackageInfo)now, threshold, column, onlyDiffs);
        } else {
            return getProjectClassesMetricsDiff((BaseProjectInfo)then, (BaseProjectInfo)now, threshold, column, onlyDiffs);
        }
    }


    public static List<MetricsDiffSummary> getProjectClassesMetricsDiff(
            final BaseProjectInfo then, final BaseProjectInfo now,
            final  Percentage threshold, final Column column, final boolean onlyDiffs) throws CloverException {

        final List<MetricsDiffSummary> diffs = newLinkedList();
        if (onlyDiffs) {
            for (PackageInfo p1 : then.getAllPackages()) {
                PackageInfo p2 = now.getNamedPackage(p1.getName());
                if (p2 != null) {
                    Logger.getInstance().debug("diffing classes in package " + p1.getName());
                    diffs.addAll(getClassesMetricsDifference(p1, p2, threshold, column, true));
                }
            }
        } else {
            for (PackageInfo p2 : now.getAllPackages()) {
                PackageInfo p1 = then.getNamedPackage(p2.getName());
                if (p1 != null) {
                    //When the package existed before and does now too, diff the contained classes
                    Logger.getInstance().debug("looking for new classes in package " + p2.getName());
                    diffs.addAll(getClassesMetricsDifference(p1, p2, threshold, column, false));
                } else {
                    //When the package is new, all classes are considered added
                    for (ClassInfo classInfo : p2.getClasses()) {
                        diffs.add(getNewClassMetrics(null, classInfo, column));
                    }
                }
            }
        }
        diffs.sort(MetricsDiffSummary.DIFF_COMP);
        return diffs;
    }

    public static List<MetricsDiffSummary> getPackageClassesMetricsDiff(BasePackageInfo then, BasePackageInfo now,
                                                     Percentage threshold, Column column, boolean onlyDiffs) throws CloverException {
        if (!then.getName().equals(now.getName())) {
            throw new IllegalArgumentException("Can't compare different packages");
        }
        List<MetricsDiffSummary> diffs = newLinkedList();

        if (onlyDiffs) {
            final List<? extends ClassInfo> classList = then.getClasses();
            for (final ClassInfo c1 : classList) {
                final ClassInfo c2 = now.getContainingProject().findClass(c1.getQualifiedName());
                final MetricsDiffSummary diff = getClassMetricsDiff(c1, c2, threshold, column);
                if (diff != null) {
                    diffs.add(diff);
                }
            }
        }
        else {
            final List<? extends ClassInfo> classList = now.getClasses();
            for (final ClassInfo c2 : classList) {
                final ClassInfo c1 = then.getContainingProject().findClass(c2.getQualifiedName());
                final MetricsDiffSummary diff = getNewClassMetrics(c1, c2, column);
                if (diff != null) {
                    diffs.add(diff);
                }
            }
        }

        diffs.sort(MetricsDiffSummary.DIFF_COMP);
        return diffs;
    }

    public static MetricsDiffSummary getClassMetricsDiff(ClassInfo c1, ClassInfo c2, Percentage threshold, Column column) throws CloverException {
        if (c2 != null) {
            Logger.getInstance().debug("diffing " + column.getTitle() + " for " + c1.getQualifiedName());

            Column col1 = column.copy();
            Column col2 = column.copy();

            col1.init(c1.getMetrics());
            col2.init(c2.getMetrics());
            if (column.getColumnData() == null) {
                column.init(c1.getMetrics());
            }
            if (col1.getNumber().doubleValue() > column.getNumber().doubleValue()) {
                column.init(c1.getMetrics());
            }
            if (col2.getNumber().doubleValue() > column.getNumber().doubleValue()) {
                column.init(c2.getMetrics());
            }

            Number pc1 = col1.getNumber();
            Number pc2 = col2.getNumber();

            if (!pc1.equals(pc2)) {
                if (pc1.intValue() == -1) {
                    pc1 = 0;
                } else if (pc2.intValue() == -1) {
                    pc2 = 0;
                }
            }

            float diff = pc2.floatValue() - pc1.floatValue();
            if (Math.abs(diff) >= threshold.getValue().floatValue()) {
                Logger.getInstance().debug("added " + c1.getQualifiedName());
                return new MetricsDiffSummary(c1, c2, pc1, pc2, diff, column);
            }
        }
        return null;
    }

    public static MetricsDiffSummary getNewClassMetrics(ClassInfo c1, ClassInfo c2, Column column)
            throws CloverException {
        Column col2 = column.copy();
        col2.init(c2.getMetrics());
        if (column.getColumnData() == null) {
            column.init(c2.getMetrics());
        }
        if (col2.getNumber().doubleValue() > column.getNumber().doubleValue()) {
            column.init(c2.getMetrics());
        }

        if (c1 == null) {
            Logger.getInstance().debug("found new " + column.getTitle() + " for " + c2.getQualifiedName());

            Number pc2 = col2.getNumber();
            Number pc1 = 100;
            if (pc2.intValue() == -1) {
                pc2 = 0;
            }
            return new MetricsDiffSummary(null, c2, pc1, pc2, pc2.floatValue(), column);
        }
        return null;
    }

}
