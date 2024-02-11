package org.openclover.core.reporters.html;

import org.openclover.core.registry.entities.BaseClassInfo;

import java.util.Comparator;

public class OrderedCalculatorComparator implements Comparator<BaseClassInfo> {
    private final ClassInfoStatsCalculator[] calculators;

    public OrderedCalculatorComparator(ClassInfoStatsCalculator[] calculators) {
        this.calculators = calculators;
    }

    @Override
    public int compare(BaseClassInfo object, BaseClassInfo object1) {
        for (ClassInfoStatsCalculator calculator : calculators) {
            int value = cmp(calculator, object, object1);
            if (value != 0) {
                return value;
            }
        }
        return 0;
    }

    private int cmp(ClassInfoStatsCalculator c, BaseClassInfo classInfo, BaseClassInfo classInfo1) {
        int scaledValue = c.getScaledValue(classInfo);
        int scaledValue1 = c.getScaledValue(classInfo1);
        return scaledValue1 - scaledValue;
    }
}
