package org.openclover.eclipse.core.views.testcontributions;

import org.eclipse.ui.IPersistable;
import org.eclipse.ui.IMemento;

public abstract class TestContributionsWidgetSettings
    implements IPersistable
{
    public static final String COL_WIDTH_PREFIX = "column.width.";
    public static final String REVERSE_SORT = "reverse.sort";
    private static final String SORTED_COLUMN = "sorted.column";

    private int[] columnWidths;
    private int sortedColumn;
    private boolean reverseSort;

    public TestContributionsWidgetSettings() {
        this(null);
    }

    public TestContributionsWidgetSettings(IMemento memento) {
        columnWidths = defaultColumnWidths();
        sortedColumn = 0;
        reverseSort = false;

        if (memento != null) {

            Integer sortedColumn = memento.getInteger(prefix(SORTED_COLUMN));
            if (sortedColumn != null) {
                this.sortedColumn = sortedColumn.intValue();
            }

            String reverseSort = memento.getString(prefix(REVERSE_SORT));
            if (reverseSort != null) {
                this.reverseSort = Boolean.valueOf(reverseSort).booleanValue();
            }

            for (int i = 0; i < columnWidths.length; i++) {
                Integer width = memento.getInteger(prefix(COL_WIDTH_PREFIX + i));
                if (width != null) {
                    columnWidths[i] = width.intValue();
                }
            }
        }
    }

    @Override
    public void saveState(IMemento memento) {
        memento.putInteger(prefix(SORTED_COLUMN), sortedColumn);
        memento.putString(prefix(REVERSE_SORT), Boolean.toString(reverseSort));
        for (int i = 0; i < columnWidths.length; i++) {
            memento.putInteger(prefix(COL_WIDTH_PREFIX + i), columnWidths[i]);
        }
    }

    public abstract String prefix(String base);

    private int[] defaultColumnWidths() {
        return new int[] { 300 };
    }

    public void setColumnSize(int columnNumber, int width) {
        columnWidths[columnNumber] = width;
    }

    public int getColumnSize(int columnNumber) {
        return columnWidths[columnNumber];
    }

    public void sortOn(int columnNumber) {
        reverseSort = reverseSort ^ (sortedColumn == columnNumber);
        sortedColumn = columnNumber;
    }

    public int getSortedColumn() {
        return sortedColumn;
    }

    public boolean isReverseSort() {
        return reverseSort;
    }
}
