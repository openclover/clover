package org.openclover.idea.treetables;

import com.intellij.util.ui.ColumnInfo;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ModelSorter {
    private final int SORT_ASCENDING = 1;
    private final int SORT_DESCENDING = 2;

    private int sortingType = SORT_ASCENDING;
    private int sortedColumnIndex = -1;

    private final ColumnInfo[] columns;

    public ModelSorter(ColumnInfo[] columnInfos) {
        this.columns = columnInfos;
    }

    @SuppressWarnings({"unchecked"})
    public void sortNodes(List<? extends TreeNode> nodes) {
        if (sortedColumnIndex < 0 || sortedColumnIndex >= columns.length) {
            return;
        }
        final Comparator<TreeNode> columnComparator = columns[sortedColumnIndex].getComparator();
        if (columnComparator == null) {
            return;
        }

        Comparator<TreeNode> actualComparator = sortingType == SORT_ASCENDING ?
                columnComparator : (o1, o2) -> columnComparator.compare(o2, o1);

        nodes.sort(actualComparator);
    }

    public int getSortingType() {
        return sortingType;
    }

    public int getSortedColumnIndex() {
        return sortedColumnIndex;
    }

    public void sortByColumn(int sortBy) {
        if (sortedColumnIndex == sortBy) {
            sortingType = sortingType == SORT_ASCENDING ? SORT_DESCENDING : SORT_ASCENDING;
        } else {
            sortingType = SORT_ASCENDING;
            sortedColumnIndex = sortBy;
        }
    }
}
