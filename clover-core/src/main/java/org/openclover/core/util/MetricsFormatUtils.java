package org.openclover.core.util;

import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.registry.metrics.BlockMetrics;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.registry.metrics.ProjectMetrics;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.registry.metrics.FileMetrics;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MetricsFormatUtils {
    //TODO: localization
    public static final String NO_METRICS_LABEL = "-";
    public static final String ERROR_METRICS_LABEL = "Error";

    public static String format100PcCoverage() {
        return getPercentFormatter().format(1f);
    }

    private static NumberFormat getPercentFormatter() {
        NumberFormat format = DecimalFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format;
    }

    private static NumberFormat getDecimalFormatter() {
        NumberFormat format = DecimalFormat.getNumberInstance();
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format;
    }

    private static NumberFormat getIntegerFormatter() {
        return DecimalFormat.getIntegerInstance();
    }

    public static String formatMetricsDecimal(double value) {
        //TODO: thread-safety of number format?
        //TODO: localisation?
        if (value == -1 || Double.isNaN(value) || Double.isInfinite(value)) {
            return NO_METRICS_LABEL;
        } else {
            return getDecimalFormatter().format(value);
        }
    }

    public static String formatMetricsPercent(double value) {
        //TODO: thread-safety of number format?
        //TODO: localisation?
        if (value == -1 || Double.isNaN(value) || Double.isInfinite(value)) {
            return NO_METRICS_LABEL;
        } else {
            return getPercentFormatter().format(value);
        }
    }

    public static String formatMetricsInteger(long value) {
        if (value == Long.MIN_VALUE) {
            return NO_METRICS_LABEL;
        } else {
            return getIntegerFormatter().format(value);
        }
    }

    public static float getAvgMethodComplexity(BlockMetrics metrics) {
        float complexity = -1f;
        if (metrics instanceof ClassMetrics) {
            complexity = ((ClassMetrics)metrics).getAvgMethodComplexity();
        }
        return complexity;
    }

    public static int getComplexity(BlockMetrics metrics) {
        return metrics.getComplexity();
    }

    public static float getComplexityDensity(BlockMetrics metrics) {
        return metrics.getComplexityDensity();
    }

    public static long getNumPackages(BlockMetrics metrics) {
        long numPackages = Long.MIN_VALUE ;
        if (metrics instanceof ProjectMetrics) {
            numPackages = ((ProjectMetrics)metrics).getNumPackages();
        }
        return numPackages;
    }

    public static long getNumMethods(BlockMetrics metrics) {
        long numMethods = Long.MIN_VALUE ;
        if (metrics instanceof ClassMetrics) {
            numMethods = ((ClassMetrics)metrics).getNumMethods();
        }
        return numMethods;
    }

    public static long getNumFiles(BlockMetrics metrics) {
        long numFiles = Long.MIN_VALUE ;
        if (metrics instanceof PackageMetrics) {
            numFiles = ((PackageMetrics)metrics).getNumFiles();
        }
        return numFiles;
    }

    public static long getNcLineCount(BlockMetrics metrics) {
        long ncLineCount = Long.MIN_VALUE ;
        if (metrics instanceof FileMetrics) {
            ncLineCount = ((FileMetrics)metrics).getNcLineCount();
        }
        return ncLineCount;
    }

    public static long getNumClasses(BlockMetrics metrics) {
        long numClasses = Long.MIN_VALUE ;
        if (metrics instanceof FileMetrics) {
            numClasses = ((FileMetrics)metrics).getNumClasses();
        }
        return numClasses;
    }

    public static long getLineCount(BlockMetrics metrics) {
        long lineCount = Long.MIN_VALUE ;
        if (metrics instanceof FileMetrics) {
            lineCount = ((FileMetrics)metrics).getLineCount();
        }
        return lineCount;
    }

    public static long getNumStatements(BlockMetrics metrics) {
        long numStatements = Long.MIN_VALUE ;
        if (metrics != null) {
            numStatements = metrics.getNumStatements();
        }
        return numStatements;
    }

    public static long getNumBranches(BlockMetrics metrics) {
        long numBranches = Long.MIN_VALUE ;
        if (metrics != null) {
            numBranches = metrics.getNumBranches();
        }
        return numBranches;
    }

    public static String textForCoverage(ElementInfo info) {
        if (info instanceof BranchInfo) {
            BranchInfo branchInfo = ((BranchInfo) info);
            return
                    "Line " + info.getStartLine() + ": Expression evaluated to true "
                            + branchInfo.getTrueHitCount()
                            + " time" + (branchInfo.getTrueHitCount() == 1 ? "" : "s") + ", false "
                            + branchInfo.getFalseHitCount()
                            + " time" + (branchInfo.getFalseHitCount() == 1 ? "" : "s") + ".";
        } else if (info instanceof StatementInfo) {
            return
                    "Line " + info.getStartLine() + ": Statement executed "
                            + info.getHitCount()
                            + " time" + (info.getHitCount() == 1 ? "" : "s") + ".";
        } else if (info instanceof MethodInfo) {
            return
                    "Line " + info.getStartLine() + ": " + (((MethodInfo) info).isTest() ? "Test method" : "Method")
                            + " executed "
                            + info.getHitCount()
                            + " time" + (info.getHitCount() == 1 ? "" : "s") + ".";
        } else {
            return "";
        }
    }
}
