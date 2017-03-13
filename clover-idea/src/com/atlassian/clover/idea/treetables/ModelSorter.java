package com.atlassian.clover.idea.treetables;

import com.intellij.util.ui.ColumnInfo;

import javax.swing.tree.DefaultMutableTreeNode;
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
    public void sortNodes(List<DefaultMutableTreeNode> nodes) {
        if (sortedColumnIndex < 0 || sortedColumnIndex >= columns.length) {
            return;
        }
        final Comparator<DefaultMutableTreeNode> columnComparator = columns[sortedColumnIndex].getComparator();
        if (columnComparator == null) {
            return;
        }

        Comparator<DefaultMutableTreeNode> actualComparator = sortingType == SORT_ASCENDING ?
                columnComparator : new Comparator<DefaultMutableTreeNode>() {
            @Override
            public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
                return columnComparator.compare(o2, o1);
            }
        };

        Collections.sort(nodes, actualComparator);
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
