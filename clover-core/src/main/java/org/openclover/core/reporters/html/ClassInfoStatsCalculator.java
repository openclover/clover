package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.registry.entities.BaseClassInfo;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.runtime.util.Formatting;

public abstract class ClassInfoStatsCalculator {
    public abstract String getName();
    public abstract String getFormattedValue(ClassInfo classInfo);
    public abstract int getScaledValue(ClassInfo classInfo);
    public abstract boolean ignore(ClassInfo classInfo);

    public static class ElementCountCalculator extends ClassInfoStatsCalculator {
        public float getValue(ClassInfo classInfo) {
            return classInfo.getMetrics().getNumElements();
        }

        @Override
        public String getFormattedValue(ClassInfo classInfo) {
            return Formatting.formatInt((int) getValue(classInfo));
        }

        @Override
        public int getScaledValue(ClassInfo classInfo) {
            return (int) getValue(classInfo);
        }

        @Override
        public String getName() {
            return "# Elements";
        }

        @Override
        public boolean ignore(ClassInfo classInfo) {
            return getValue(classInfo) <= 0;
        }
    }

    public static class CoveredElementsCalculator extends ClassInfoStatsCalculator {
        @Override
        public boolean ignore(ClassInfo classInfo) {
            return getValue(classInfo) <= 0;
        }

        public float getValue(ClassInfo classInfo) {
            return classInfo.getMetrics().getNumCoveredElements();
        }

        @Override
        public String getFormattedValue(ClassInfo classInfo) {
            return Formatting.formatInt((int) getValue(classInfo));
        }

        @Override
        public int getScaledValue(ClassInfo classInfo) {
            return (int) getValue(classInfo);
        }

        @Override
        public String getName() {
            return "# Elements Covered";
        }
    }

    public static class AvgMethodComplexityCalculator extends ClassInfoStatsCalculator {
        @Override
        public boolean ignore(ClassInfo classInfo) {
            return classInfo.getMetrics().getNumElements() <= 0;
        }

        public float getValue(ClassInfo classInfo) {
            final float value =
                    ((ClassMetrics)classInfo.getMetrics()).getAvgMethodComplexity();
            return value > 0 ? value : 0;
        }
        @Override
        public String getFormattedValue(ClassInfo classInfo) {
            return Formatting.format3d(getValue(classInfo));
        }
        @Override
        public int getScaledValue(ClassInfo classInfo) {
            return (int) (getValue(classInfo) * 100f);
        }

        @Override
        public String getName() {
            return "Average Method Complexity";
        }
    }

    public static class PcCoveredElementsCalculator extends ClassInfoStatsCalculator {
        @Override
        public boolean ignore(ClassInfo classInfo) {
            return (getScaledValue(classInfo) == 0) || (getScaledValue(classInfo) < 0);
        }

        public float getValue(ClassInfo classInfo) {
            return classInfo.getMetrics().getPcCoveredElements();
        }
        @Override
        public String getFormattedValue(ClassInfo classInfo) {
            return Formatting.getPercentStr(classInfo.getMetrics().getPcCoveredElements());
        }
        @Override
        public int getScaledValue(ClassInfo classInfo) {
            return (int) (getValue(classInfo) * 100f);
        }

        @Override
        public String getName() {
            return "Coverage";
        }
    }
}


