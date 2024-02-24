package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ClassMetadata;

import java.util.Comparator;

public class OrderedCalculatorComparator implements Comparator<ClassInfo> {
    private final ClassInfoStatsCalculator[] calculators;

    public OrderedCalculatorComparator(ClassInfoStatsCalculator[] calculators) {
        this.calculators = calculators;
    }

    @Override
    public int compare(ClassInfo object, ClassInfo object1) {
        for (ClassInfoStatsCalculator calculator : calculators) {
            int value = cmp(calculator, object, object1);
            if (value != 0) {
                return value;
            }
        }
        return 0;
    }

    private int cmp(ClassInfoStatsCalculator c, ClassInfo classInfo, ClassInfo classInfo1) {
        int scaledValue = c.getScaledValue(classInfo);
        int scaledValue1 = c.getScaledValue(classInfo1);
        return scaledValue1 - scaledValue;
    }
}
