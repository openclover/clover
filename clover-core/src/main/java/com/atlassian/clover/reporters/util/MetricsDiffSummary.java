package com.atlassian.clover.reporters.util;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.reporters.Column;

import java.util.Comparator;


public class MetricsDiffSummary {

    public final static Comparator<MetricsDiffSummary> DIFF_COMP = new Comparator<MetricsDiffSummary>() {
        @Override
        public int compare(MetricsDiffSummary aObj1, MetricsDiffSummary aObj2) {
            if (aObj1 == null && aObj2 == null) {
                return 0;
            } else if (aObj1 == null) {
                return -1;
            } else if (aObj2 == null) {
                return 1;
            } else {
                float d1 = aObj1.getPcDiff();
                float d2 = aObj2.getPcDiff();
                if (d1 == d2) {
                    // secondary lexigraphical sort
                    return aObj1.getName().compareTo(aObj2.getName());
                } else if (d1 > d2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    };

    public final static Comparator<MetricsDiffSummary> INVERSE_DIFF_COMP = new Comparator<MetricsDiffSummary>() {
        @Override
        public int compare(MetricsDiffSummary o1, MetricsDiffSummary o2) {
            return DIFF_COMP.compare(o2, o1);
        }
    };

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
