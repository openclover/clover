package org.openclover.core.reporters.html;

import org.openclover.core.registry.entities.BaseClassInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;

public class StatisticsClassInfoVisitor {
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;
    private final Map<BaseClassInfo, String> classes = new LinkedHashMap<>();
    private final ClassInfoStatsCalculator calculator;

    public StatisticsClassInfoVisitor(ClassInfoStatsCalculator calculator) {
        this.calculator = calculator;
    }

    public void visitClassInfo(BaseClassInfo classInfo) {
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

    public List<BaseClassInfo> getClasses() {
        return newArrayList(classes.keySet());
    }

    public boolean hasClassInfo(BaseClassInfo info) {
        return classes.containsKey(info);
    }

    public ClassInfoStatsCalculator getCalculator() {
        return calculator;
    }

    public static StatisticsClassInfoVisitor visit(List<BaseClassInfo> classes, ClassInfoStatsCalculator statsCalculator) {
        final StatisticsClassInfoVisitor visitor = new StatisticsClassInfoVisitor(statsCalculator);
        for (BaseClassInfo cls : classes) {
            visitor.visitClassInfo(cls);
        }
        return visitor;
    }
}