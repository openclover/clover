package org.openclover.eclipse.core.views.widgets.columns;

import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.CustomColumnDefinition;

import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.Collections;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Sets.newHashSet;

class ColumnDefinitionsModel {
    private LinkedHashSet<ColumnDefinition> remaining;
    private LinkedHashSet<ColumnDefinition> assigned;
    private LinkedHashSet<PropertyChangeListener> listeners;
    private LinkedHashSet<ColumnDefinition> allColumns;

    public ColumnDefinitionsModel(ColumnDefinition[] selectedColumns, ColumnDefinition[] allColumns) {
        this.allColumns = new LinkedHashSet<>(Arrays.asList(allColumns));
        this.listeners = new LinkedHashSet<>();
        this.assigned = new LinkedHashSet<>(Arrays.asList(selectedColumns));
        this.remaining = new LinkedHashSet<>(Arrays.asList(allColumns));
        this.remaining.removeAll(this.assigned);
    }

    public void addListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        listeners.clear();
        remaining.clear();
        assigned.clear();
    }

    public List<ColumnDefinition> getAssigned() {
        return newLinkedList(assigned);
    }

    private static Set<ColumnDefinition> getColumns(Set<ColumnDefinition> columns, boolean customColumns) {
        Set<ColumnDefinition> clippedColumns = new LinkedHashSet<>(columns);
        clippedColumns.removeIf(columnDefinition -> customColumns ^ columnDefinition.isCustom());
        return clippedColumns;
    }

    public Set<ColumnDefinition> getRemainingBuiltins() {
        return getColumns(remaining, false);
    }

    public Set<ColumnDefinition> getRemainingCustoms() {
        return getColumns(remaining, true);
    }

    public void assign(ColumnDefinition column) {
        remaining.remove(column);
        assigned.add(column);
        fireModelChanged();
    }

    public void assignAll(boolean customColumns) {
        Set<ColumnDefinition> toBeAssigned = getColumns(remaining, customColumns);
        assigned.addAll(toBeAssigned);
        remaining.removeAll(toBeAssigned);
        fireModelChanged();
    }

    public boolean deassignAll() {
        Set<ColumnDefinition> toBeDeassigned = newHashSet(assigned);
        toBeDeassigned.removeIf(ColumnDefinition::isLocked);

        boolean allDeassigned = toBeDeassigned.size() == assigned.size();
        assigned.removeAll(toBeDeassigned);
        remaining.addAll(toBeDeassigned);
        fireModelChanged();
        return allDeassigned;
    }

    public boolean deassign(ColumnDefinition column) {
        if (!column.isLocked()) {
            remaining.add(column);
            assigned.remove(column);
            fireModelChanged();
            return true;
        } else {
            return false;
        }
    }

    private void fireModelChanged() {
        PropertyChangeEvent event = new PropertyChangeEvent(this, "*", null, null);
        for (PropertyChangeListener listener : listeners) {
            listener.propertyChange(event);
        }
    }

    public Set<ColumnDefinition> getCustomColumns() {
        return getColumns(allColumns, true);
    }

    public void addCustomColumn(CustomColumnDefinition column) {
        allColumns.add(column);
        remaining.add(column);
        fireModelChanged();
    }

    public void replaceCustomColumn(CustomColumnDefinition original, CustomColumnDefinition updated) {
        allColumns = replace(allColumns, original, updated);
        remaining = replace(remaining, original, updated);
        assigned = replace(assigned, original, updated);
        fireModelChanged();
    }

    public void removeCustomColumn(CustomColumnDefinition customColumnDefinition) {
        allColumns.remove(customColumnDefinition);
        remaining.remove(customColumnDefinition);
        assigned.remove(customColumnDefinition);
        fireModelChanged();
    }

    public void moveAssignedUp(ColumnDefinition column) {
        assigned = move(assigned, column, -1);
        fireModelChanged();
    }

    public void moveAssignedDown(ColumnDefinition column) {
        assigned = move(assigned, column, 1);
        fireModelChanged();
    }

    private LinkedHashSet<ColumnDefinition> move(LinkedHashSet<ColumnDefinition> set, ColumnDefinition column, int movement) {
        ColumnDefinition[] objects = set.toArray(new ColumnDefinition[set.size()]);
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == column) {
                ColumnDefinition temp = objects[i];
                objects[i] = objects[i + movement];
                objects[i + movement] = temp;
                break;
            }
        }
        return new LinkedHashSet<>(Arrays.asList(objects));
    }

    private LinkedHashSet<ColumnDefinition> replace(LinkedHashSet<ColumnDefinition> set, ColumnDefinition orig, ColumnDefinition updated) {
        LinkedList<ColumnDefinition> list = newLinkedList(set);
        Collections.replaceAll(list, orig, updated);
        return new LinkedHashSet<>(list);
    }

    public Set<ColumnDefinition> getBuiltintColumns() {
        return getColumns(allColumns, false);
    }
}
