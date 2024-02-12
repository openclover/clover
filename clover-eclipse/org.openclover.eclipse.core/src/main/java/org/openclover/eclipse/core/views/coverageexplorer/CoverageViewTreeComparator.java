package org.openclover.eclipse.core.views.coverageexplorer;

import org.eclipse.jface.viewers.Viewer;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.ExplorerViewComparator;

public class CoverageViewTreeComparator extends ExplorerViewComparator {
    public static CoverageViewTreeComparator createFor(final CoverageViewSettings settings) {
        final ColumnDefinition column = settings.getTreeColumnSettings().getSortedColumn();
        return new CoverageViewTreeComparator() {
            @Override
            public int compare(Viewer viewer, Object value1, Object value2) {
                return invert(
                    settings.getTreeColumnSettings().isReverseSort(),
                    column.getComparator(settings, settings.getMetricsScope()).compare(value1, value2));
            }
        };
    }
}
