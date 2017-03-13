package com.atlassian.clover.eclipse.core.views;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;
import com.atlassian.clover.registry.metrics.BlockMetrics;
import org.eclipse.jface.viewers.ViewerComparator;

public abstract class ExplorerViewComparator extends ViewerComparator {
    protected static final Double NOT_AVAILABLE_DOUBLE = -1.0;

    public static int compareName(Object value1, Object value2) {
        return nameOf(value1).compareToIgnoreCase(nameOf(value2));
    }

    private static String nameOf(Object value) {
        String name1 =
            (value instanceof IJavaElement
                ? ((IJavaElement)value).getElementName()
                : (value instanceof IProject)
                    ? ((IProject)value).getName()
                    : (value instanceof IAdaptable)
                        ? nameOf(((IAdaptable)value).getAdapter(IJavaElement.class))
                        : String.valueOf(value));
        return name1;
    }

    protected int invert(boolean reverseIt, int comparisonResult) {
        return (reverseIt ? -1 : 1) * comparisonResult;
    }

    protected static int compareCustomColumn(MetricsScope scope, CustomColumnDefinition column, Object value1, Object value2) {
        BlockMetrics metrics1 = scope.getMetricsFor(value1);
        BlockMetrics metrics2 = scope.getMetricsFor(value2);
        Double custom1;
        try {
            custom1 = metrics1 == null ? NOT_AVAILABLE_DOUBLE : new Double(column.calculate(metrics1));
        } catch (Exception e) {
            custom1 = NOT_AVAILABLE_DOUBLE;
        }
        Double custom2;
        try {
            custom2 = metrics2 == null ? NOT_AVAILABLE_DOUBLE : new Double(column.calculate(metrics2));
        } catch (Exception e) {
            custom2 = NOT_AVAILABLE_DOUBLE;
        }
        return  custom1.compareTo(custom2);
    }

    public static int compareType(Object object1, Object object2) {
        object1 = sanitizeType(object1);
        object2 = sanitizeType(object2);
        int value1 = typeValueFor(object1);
        int value2 = typeValueFor(object2);
        return
            value1 < value2
                ? -1
                : value1 > value2
                    ? 1
                    : 1;
    }

    private static int typeValueFor(Object object) {
        if (object instanceof IProject) {
            return 0;
        } else if (object instanceof IPackageFragmentRoot) {
            return 1;
        } else if (object instanceof IPackageFragment) {
            return 2;
        } else if (object instanceof ICompilationUnit) {
            return 3;
        } else if (object instanceof IType) {
            return 4;
        } else if (object instanceof IJavaElement) {
            return 5;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    private static Object sanitizeType(Object object) {
        return
            (object instanceof IJavaElement)
                ? (IJavaElement)object
                : (object instanceof IAdaptable)
                    ? (IJavaElement)((IAdaptable)object).getAdapter(IJavaElement.class)
                    : object;
    }
}
