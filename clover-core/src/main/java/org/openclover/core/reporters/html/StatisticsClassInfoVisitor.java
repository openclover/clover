package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.registry.entities.BaseClassInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;

public class StatisticsClassInfoVisitor {
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;
    private final Map<ClassInfo, String> classes = new LinkedHashMap<>();
    private final ClassInfoStatsCalculator calculator;

    public StatisticsClassInfoVisitor(ClassInfoStatsCalculator calculator) {
        this.calculator = calculator;
    }

    public void visitClassInfo(ClassInfo classInfo) {
        if (!calculator.ignore(classInfo)) {
            int count = calculator.getScaledValue(classInfo);

            classes.put(classInfo, calculator.getFormattedValue(classInfo));

            if (count <= min) {
                min = count;
            }
            if (count >= max) {
                max = count;
            }
        }
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public long getRange() {
        return max - min;
    }

    public List<ClassInfo> getClasses() {
        return newArrayList(classes.keySet());
    }

    public boolean hasClassInfo(BaseClassInfo info) {
        return classes.containsKey(info);
    }

    public ClassInfoStatsCalculator getCalculator() {
        return calculator;
    }

    public static StatisticsClassInfoVisitor visit(List<? extends ClassInfo> classes, ClassInfoStatsCalculator statsCalculator) {
        final StatisticsClassInfoVisitor visitor = new StatisticsClassInfoVisitor(statsCalculator);
        for (ClassInfo cls : classes) {
            visitor.visitClassInfo(cls);
        }
        return visitor;
    }
}