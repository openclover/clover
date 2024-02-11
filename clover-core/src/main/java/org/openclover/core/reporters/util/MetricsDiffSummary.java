package org.openclover.core.reporters.util;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.reporters.Column;
import org.openclover.runtime.api.CloverException;

import java.util.Comparator;


public class MetricsDiffSummary {

    public final static Comparator<MetricsDiffSummary> DIFF_COMP = (metricsDiff1, metricsDiff2) -> {
        if (metricsDiff1 == null && metricsDiff2 == null) {
            return 0;
        } else if (metricsDiff1 == null) {
            return -1;
        } else if (metricsDiff2 == null) {
            return 1;
        } else {
            float d1 = metricsDiff1.getPcDiff();
            float d2 = metricsDiff2.getPcDiff();
            if (d1 == d2) {
                // secondary lexigraphical sort
                return metricsDiff1.getName().compareTo(metricsDiff2.getName());
            } else if (d1 > d2) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    public final static Comparator<MetricsDiffSummary> INVERSE_DIFF_COMP = (metricsDiff1, metricsDiff2) ->
            DIFF_COMP.compare(metricsDiff2, metricsDiff1);

    private ClassInfo classThen;
    private ClassInfo classNow;
    private Number pcThen;
    private Number pcNow;
    private float diff;
    private Column column;

    public MetricsDiffSummary(ClassInfo classThen, ClassInfo classNow, Number pcThen, Number pcNow, float diff, Column column) {
        this.classThen = classThen;
        this.classNow = classNow;
        this.pcThen = pcThen;
        this.pcNow = pcNow;
        this.diff = diff;
        this.column = column;
    }

    public Column getColumn() {
        return column;
    }

    public String getString1() throws CloverException {
        return getString(classThen);
    }

    /**
     * called by hist-mover-row.vm
     */
    @SuppressWarnings("unused")
    public String getString2() throws CloverException {
        return getString(classNow);
    }

    private String getString(ClassInfo classInfo) throws CloverException {
        Column col = column.copy();
        col.init(classInfo.getMetrics());

        return column.getFormat().format(col.getColumnData());
    }

    public float getPcDiff() {
        return diff;
    }

    public Number getPc1() {
        return pcThen;
    }

    public Number getPc2() {
        return pcNow;
    }

    public float getPc2float() {
        return getPc2().floatValue() / 100f;
    }

    public float getPc1float() {
        return getPc1().floatValue() / 100f;
    }

    public ClassInfo getCurrentClassInfo() {
        return classNow;
    }

    public String getName() {
        return classNow.getQualifiedName();
    }

    public String toString() {
        return pcThen + " " + getName() + " " + pcNow + "  (" + (diff >= 0 ? "+" : "") + diff + ")";
    }

}
