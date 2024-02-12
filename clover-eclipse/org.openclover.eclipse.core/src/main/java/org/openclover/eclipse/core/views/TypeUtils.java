package org.openclover.eclipse.core.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.openclover.core.registry.metrics.BlockMetrics;
import org.openclover.core.registry.metrics.ClassMetrics;

public class TypeUtils {
    public static boolean isPackageFragmentRoot(Object element) {
        return element instanceof IPackageFragmentRoot;
    }

    public static boolean unavailableCoverage(BlockMetrics metrics) {
        return metrics == null || metrics.getNumElements() == 0;
    }

    public static boolean isProject(Object element) {
        return element instanceof IProject;
    }

    public static boolean isType(Object element) {
        return element instanceof IType;
    }

    public static boolean isCUOrType(Object element) {
        return
            (element instanceof IType || element instanceof ICompilationUnit);
    }

    public static boolean isUnitTest(Object element, BlockMetrics metrics) {
        return
            (metrics != null
                && isCUOrType(element)
                && ((ClassMetrics)metrics).getNumTestMethods() > 0);
    }
}