package org.openclover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.CustomColumnDefinition;
import org.openclover.eclipse.core.views.widgets.columns.ColumnSelectionDialog;

import java.util.List;
import java.util.Set;

public abstract class ConfigureColumnsActionDelegate extends CloverProjectActionDelegate {
    @Override
    protected void updateStateForSelection(IAction action) {
        action.setEnabled(true);
    }

    @Override
    public void run(IAction action) {
        if (action.isEnabled()) {
            ColumnSelectionDialog dialog = new ColumnSelectionDialog(
                getShell(),
                getViewTitle(),
                view.getTreeColumnManager().getVisibleColumns(),
                view.getTreeColumnManager().getAllColumns());

            if (dialog.open() == Dialog.OK) {
                Set customColumns = dialog.getCustomColumns();
                List visibleColumns = dialog.getVisibleColumns();
                view.getTreeColumnManager().update(
                    (CustomColumnDefinition[])customColumns.toArray(new CustomColumnDefinition[customColumns.size()]),
                    (ColumnDefinition[])visibleColumns.toArray(new ColumnDefinition[visibleColumns.size()]));
            }
        }
    }

    public abstract String getViewTitle();
}
