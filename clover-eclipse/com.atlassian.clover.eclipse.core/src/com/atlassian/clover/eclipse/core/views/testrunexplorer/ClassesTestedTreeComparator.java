package com.atlassian.clover.eclipse.core.views.testrunexplorer;

import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.Viewer;

import com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes.CoverageContributionNode;
import com.atlassian.clover.eclipse.core.views.ColumnCollectionSettings;
import com.atlassian.clover.eclipse.core.views.ExplorerViewComparator;
import com.atlassian.clover.eclipse.core.views.ColumnDefinition;
import com.atlassian.clover.eclipse.core.views.CustomColumnDefinition;
import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;

public abstract class ClassesTestedTreeComparator extends ExplorerViewComparator {
    public static ViewerComparator getFor(final ColumnCollectionSettings settings) {
        final ColumnDefinition column = settings.getSortedColumn();
        if (column == TestRunExplorerView.CONTRIB_COL_CLASS) {
            return new ClassesTestedTreeComparator() {
                @Override
                public int compare(Viewer viewer, Object value1, Object value2) {
                    return invert(settings.isReverseSort(), compareName(value1, value2));
                }
            };
        } else if (column == TestRunExplorerView.CONTRIB_COL_CONTRIB) {
            return new ClassesTestedTreeComparator() {
                @Override
                public int compare(Viewer viewer, Object value1, Object value2) {
                    return invert(settings.isReverseSort(), compareContribCoverage(value1, value2));
                }
            };
        } else if (column == TestRunExplorerView.CONTRIB_COL_UNIQUE) {
            return new ClassesTestedTreeComparator() {
                @Override
                public int compare(Viewer viewer, Object value1, Object value2) {
                    return invert(settings.isReverseSort(), compareUniqueCoverage(value1, value2));
                }
            };
        } else if (column.isCustom()) {
            return new ClassesTestedTreeComparator() {
                @Override
                public int compare(Viewer viewer, Object value1, Object value2) {
                    return invert(settings.isReverseSort(), compareCustomColumn(MetricsScope.FULL, (CustomColumnDefinition)column, value1, value2));
                }
            };
        } else {
            return new ClassesTestedTreeComparator() {
                @Override
                public int compare(Viewer viewer, Object value1, Object value2) {
                    return invert(settings.isReverseSort(), super.compare(viewer, value1, value2));
                }
            };
        }
    }

    public static int compareName(Object o1, Object o2) {
        CoverageContributionNode cov1 =
            (CoverageContributionNode)o1;
        CoverageContributionNode cov2 =
            (CoverageContributionNode)o2;
        return cov1.getElement().getElementName().compareTo(cov2.getElement().getElementName());
    }

    public static int compareContribCoverage(Object o1, Object o2) {
        CoverageContributionNode cov1 =
            (CoverageContributionNode)o1;
        CoverageContributionNode cov2 =
            (CoverageContributionNode)o2;
        return
            cov1.getCoverage() == cov2.getCoverage()
                ? 0
                : cov1.getCoverage() < cov2.getCoverage()
                    ? -1
                    : 1;
    }

    public static int compareUniqueCoverage(Object o1, Object o2) {
        CoverageContributionNode cov1 =
            (CoverageContributionNode)o1;
        CoverageContributionNode cov2 =
            (CoverageContributionNode)o2;
        return
            cov1.getUnique() == cov2.getUnique()
                ? 0
                : cov1.getUnique() < cov2.getUnique()
                    ? -1
                    : 1;
    }
}
