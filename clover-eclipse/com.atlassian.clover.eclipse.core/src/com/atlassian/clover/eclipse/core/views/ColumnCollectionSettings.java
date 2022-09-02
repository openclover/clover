package com.atlassian.clover.eclipse.core.views;

import org.eclipse.ui.IPersistable;
import org.eclipse.ui.IMemento;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.LinkedHashSet;

import com.atlassian.clover.eclipse.core.CloverPlugin;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Lists.newLinkedList;
import static clover.com.google.common.collect.Maps.newHashMap;
import static clover.com.google.common.collect.Maps.newLinkedHashMap;
import static clover.com.google.common.collect.Sets.newLinkedHashSet;

public class ColumnCollectionSettings
    implements IPersistable {
    private static final String SORTED_COLUMN = "sorted.column";
    private static final String REVERSE_SORT = "reverse.sort";

    private static final String VISIBLE_COLUMN_PREFIX = "visible.column.";
    private static final String VISIBLE_COLUMN_COUNT = VISIBLE_COLUMN_PREFIX + "count";

    private static final String COL_WIDTH_SUFFIX = ".width";

    private static final Integer DEFAULT_CUSTOM_COLUMN_WIDTH = 100;
    
    private List<ColumnDefinition> allBuiltinColumns;
    private Map<String, ColumnDefinition> allColumnsById;
    private Set<ColumnDefinition> visibleColumns;
    private Map<ColumnDefinition, Integer> visibleColumnsToWidths;
    private ColumnDefinition sortedColumn;
    private boolean reverseSort;
    private String propertyPrefix;

    public ColumnCollectionSettings(IMemento memento,
                                    List<ColumnDefinition> allBuiltinColumns,
                                    List<ColumnDefinition> defaultBuiltinColumns,
                                    List<Integer> defaultBuiltinWidths) {
        this("", memento, allBuiltinColumns, defaultBuiltinColumns, defaultBuiltinWidths);
    }
    
    public ColumnCollectionSettings(String propertyPrefix, IMemento memento,
                                    List<ColumnDefinition> allBuiltinColumns,
                                    List<ColumnDefinition> defaultBuiltinColumns,
                                    List<Integer> defaultBuiltinWidths) {
        this.allBuiltinColumns = newArrayList(allBuiltinColumns);

        buildDefaults(propertyPrefix, defaultBuiltinColumns, defaultBuiltinWidths);

        if (memento != null) {
            loadSortColumn(memento);
            loadReverseSort(memento);
            loadColumnWidths(memento);
        }
    }

    @Override
    public void saveState(IMemento memento) {
        saveSortedColumn(memento);
        saveReverseSort(memento);
        saveColumnWidths(memento);
    }

    private void buildDefaults(String propertyPrefix,
                               List<ColumnDefinition> defaultBuiltinColumns,
                               List<Integer> defaultBuiltinWidths) {
        regenerateAllColumnsByIdMap();
        buildDefaultColumnWidths(defaultBuiltinColumns, defaultBuiltinWidths);

        this.sortedColumn = defaultBuiltinColumns.get(0);
        this.propertyPrefix = propertyPrefix;
        this.reverseSort = false;
    }

    private void buildDefaultColumnWidths(List<ColumnDefinition> defaultBuiltinColumns,
                                          List<Integer> defaultBuiltinWidths) {
        this.visibleColumns = newLinkedHashSet();
        this.visibleColumnsToWidths = newLinkedHashMap();
        for (int i = 0; i < defaultBuiltinColumns.size(); i++) {
            visibleColumnsToWidths.put(defaultBuiltinColumns.get(i), defaultBuiltinWidths.get(i));
            visibleColumns.add(defaultBuiltinColumns.get(i));
        }
    }

    private void loadColumnWidths(IMemento memento) {
        Integer columnCount = memento.getInteger(prefixed(VISIBLE_COLUMN_COUNT));
        LinkedHashMap<ColumnDefinition, Integer> visibleColumnsToWidths = newLinkedHashMap();
        LinkedHashSet<ColumnDefinition> visibleColumns = newLinkedHashSet();
        if (columnCount != null && columnCount.intValue() >= 0) {
            for (int i = 0; i < columnCount.intValue(); i++) {
                String columnId = memento.getString(prefixed(VISIBLE_COLUMN_PREFIX + i));
                ColumnDefinition column = (columnId == null ? null : allColumnsById.get(columnId));
                Integer columnWidth = memento.getInteger(prefixed(VISIBLE_COLUMN_PREFIX + i + COL_WIDTH_SUFFIX));
                if (column != null && columnWidth != null && columnWidth.intValue() > 0) {
                    visibleColumns.add(column);
                    visibleColumnsToWidths.put(column, columnWidth);
                }
            }
        }
        //There must be at least one column before we will override defaults
        if (visibleColumnsToWidths.size() > 0) {
            this.visibleColumnsToWidths = visibleColumnsToWidths;
            this.visibleColumns = visibleColumns;
        }
    }

    private void loadReverseSort(IMemento memento) {
        String reverseSort = memento.getString(prefixed(REVERSE_SORT));
        if (reverseSort != null) {
            this.reverseSort = Boolean.valueOf(reverseSort).booleanValue();
        }
    }

    private void loadSortColumn(IMemento memento) {
        String sortedColumnId = memento.getString(prefixed(SORTED_COLUMN));
        if (sortedColumnId != null) {
            ColumnDefinition firstBuiltin = allBuiltinColumns.get(0);
            if (sortedColumnId.contains(":")) {
                this.sortedColumn = allColumnsById.get(sortedColumnId);
                this.sortedColumn = this.sortedColumn == null ? firstBuiltin : this.sortedColumn;
            } else {
                this.sortedColumn = firstBuiltin;
            }
        }
    }

    private void regenerateAllColumnsByIdMap() {
        final Map<String, ColumnDefinition> allColumnsById = newHashMap();
        //Push all built-in columns into all column map
        for (ColumnDefinition builtinColumn : allBuiltinColumns) {
            allColumnsById.put(
                    builtinColumn.getId(),
                    builtinColumn);
        }

        List<CustomColumnDefinition> customColumns = CloverPlugin.getInstance().getInstallationSettings().getCustomColumns();
        for (CustomColumnDefinition customColumn : customColumns) {
            allColumnsById.put(
                    customColumn.getId(),
                    customColumn);
        }

        this.allColumnsById = allColumnsById;
    }

    private void saveColumnWidths(IMemento memento) {
        memento.putInteger(prefixed(VISIBLE_COLUMN_COUNT), visibleColumnsToWidths.size());
        List customColumns = CloverPlugin.getInstance().getInstallationSettings().getCustomColumns();
        int count = 0;
        for (Map.Entry<ColumnDefinition, Integer> entry : visibleColumnsToWidths.entrySet()) {
            ColumnDefinition colDef = entry.getKey();
            if (colDef.isCustom()) {
                memento.putString(prefixed(VISIBLE_COLUMN_PREFIX + count), CustomColumnDefinition.idForIndex(customColumns.indexOf(colDef)));
            } else {
                memento.putString(prefixed(VISIBLE_COLUMN_PREFIX + count), colDef.getId());
            }
            memento.putInteger(prefixed(VISIBLE_COLUMN_PREFIX + count + COL_WIDTH_SUFFIX), entry.getValue().intValue());
            count++;
        }
    }

    private void saveReverseSort(IMemento memento) {
        memento.putString(prefixed(REVERSE_SORT), Boolean.toString(reverseSort));
    }

    private void saveSortedColumn(IMemento memento) {
        memento.putString(prefixed(SORTED_COLUMN), sortedColumn.getId());
    }

    private String prefixed(String name) {
        return propertyPrefix + (propertyPrefix.length() > 0 ? "." : "") + name;
    }

    public void sortOn(ColumnDefinition columnDefinition) {
        reverseSort = reverseSort ^ (sortedColumn == columnDefinition);
        sortedColumn = columnDefinition;
    }

    public List getVisibleColumns() {
        return newLinkedList(visibleColumnsToWidths.keySet());
    }
    
    public Map<ColumnDefinition, Integer> getVisibleColumnsToWidths() {
        return visibleColumnsToWidths;
    }

    public Set getAllColumns() {
        return newLinkedHashSet(allColumnsById.values());
    }

    public ColumnDefinition getSortedColumn() {
        return sortedColumn;
    }

    public boolean isReverseSort() {
        return reverseSort;
    }

    public int getVisibleColumnIndexFor(ColumnDefinition column) {
        return newArrayList(visibleColumns).indexOf(column);
    }

    public ColumnDefinition columnForIndex(int columnIndex) {
        return newArrayList(visibleColumns).get(columnIndex);
    }

    public void setVisibleColumnSize(ColumnDefinition column, int width) {
        visibleColumnsToWidths.put(column, new Integer(width));
    }

    public void setColumnOrder(int[] orderIndicies) {
        List<ColumnDefinition> visibleColumns = newArrayList(this.visibleColumns);
        LinkedHashMap<ColumnDefinition, Integer> newVisibleColumnsToWidths = newLinkedHashMap();
        for (int orderIndicy : orderIndicies) {
            newVisibleColumnsToWidths.put(
                    visibleColumns.get(orderIndicy),
                    visibleColumnsToWidths.get(visibleColumns.get(orderIndicy)));
        }
        this.visibleColumnsToWidths = newVisibleColumnsToWidths;
    }

    public void update(CustomColumnDefinition[] customColumns, ColumnDefinition[] visibleColumns) {
        CloverPlugin.getInstance().getInstallationSettings().setCustomColumns(Arrays.asList(customColumns));

        regenerateAllColumnsByIdMap();

        LinkedHashSet<ColumnDefinition> newVisibleColumns = newLinkedHashSet();
        LinkedHashMap<ColumnDefinition, Integer> newVisibleColumnsToWidths = newLinkedHashMap();
        for (ColumnDefinition visibleColumn : visibleColumns) {
            Integer width = visibleColumnsToWidths.get(visibleColumn);
            width = width == null ? DEFAULT_CUSTOM_COLUMN_WIDTH : width;
            newVisibleColumnsToWidths.put(visibleColumn, width);
            newVisibleColumns.add(visibleColumn);
        }
        visibleColumnsToWidths = newVisibleColumnsToWidths;
        this.visibleColumns = newVisibleColumns;

        if (!allColumnsById.containsValue(sortedColumn)) {
            sortedColumn = allColumnsById.values().iterator().next();
            reverseSort = false;
        }
    }

    public List getAllBuiltinColumns() {
        return allBuiltinColumns;
    }
}
