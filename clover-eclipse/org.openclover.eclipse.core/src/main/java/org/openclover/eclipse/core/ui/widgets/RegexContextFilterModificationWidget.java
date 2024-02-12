package org.openclover.eclipse.core.ui.widgets;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.openclover.core.context.ContextSet;
import org.openclover.core.context.ContextStore;
import org.openclover.core.context.NamedContext;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.eclipse.core.projects.settings.RegexpEntry;
import org.openclover.eclipse.core.ui.SwtUtils;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.core.util.Lists.newArrayList;

public class RegexContextFilterModificationWidget extends Composite {
    private Button addFilter;
    private Button removeFilter;
    private Button editFilter;
    private Button copyFilter;
    private List filterList;
    private java.util.List<RegexpEntry> regexps = newArrayList();
    private int regexpsLengthSinceLastStore;
    private ProjectSettings properties;

    public RegexContextFilterModificationWidget(final Composite parent, ProjectSettings props) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, false));
        properties = props;

        Label regexExplanationLabel = new Label(this, SWT.NONE);
        regexExplanationLabel.setText(
            CloverEclipsePluginMessages.REGEXP_CONTEXT_FILTER_MODIFICATION_INSTRUCTIONS());
        SwtUtils.gridDataFor(regexExplanationLabel).horizontalSpan = 2;

        filterList = new List(this, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        GridData gd = SwtUtils.gridDataFor(filterList);
        gd.verticalSpan = 5;
        gd.horizontalAlignment = GridData.FILL;
        gd.verticalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;

        addFilter = new Button(this, SWT.PUSH);
        addFilter.setText("Add");
        gd = SwtUtils.gridDataFor(addFilter);
        gd.horizontalSpan = 1;
        gd.horizontalAlignment = GridData.FILL;

        removeFilter = new Button(this, SWT.PUSH);
        removeFilter.setText("Remove");
        removeFilter.setEnabled(false);
        gd = SwtUtils.gridDataFor(removeFilter);
        gd.horizontalSpan = 1;
        gd.horizontalAlignment = GridData.FILL;

        editFilter = new Button(this, SWT.PUSH);
        editFilter.setText("Edit");
        editFilter.setEnabled(false);
        gd = SwtUtils.gridDataFor(editFilter);
        gd.horizontalSpan = 1;
        gd.horizontalAlignment = GridData.FILL;

        copyFilter = new Button(this, SWT.PUSH);
        copyFilter.setText("Copy");
        copyFilter.setEnabled(false);
        gd = SwtUtils.gridDataFor(copyFilter);
        gd.horizontalSpan = 1;
        gd.horizontalAlignment = GridData.FILL;

        // initialise the listeners.
        addFilter.addListener(SWT.Selection, event -> {
            try {
                doAdd();
            } catch (Exception e) {
                CloverPlugin.logError("Error adding new regex", e);
                logRegexError(e);
            }
        });
        removeFilter.addListener(SWT.Selection, event -> doRemove());
        copyFilter.addListener(SWT.Selection, event -> {
            try {
                doCopy();
            } catch (Exception e) {
                CloverPlugin.logError("Error duplicating regex", e);
                logRegexError(e);
            }
        });
        editFilter.addListener(SWT.Selection, event -> doEdit());

        filterList.addListener(SWT.Selection, event -> doSelection());

        loadFrom(properties);
    }

    private void logRegexError(Exception e) {
        MessageDialog.openError(
            getShell(),
            "Regular Expression Error",
            "An error was found in the regular expressions: " + e.getMessage());
    }

    private void loadFrom(ProjectSettings properties) {
        regexps = properties.getRegexpFilters();
        regexpsLengthSinceLastStore = regexps.size();
        refreshList();
        updateButtonStates();
    }

    private void refreshList() {
        filterList.removeAll();
        for (RegexpEntry r : regexps) {
            filterList.add(r.getName() + (r.isChanged() ? " (*)" : ""));
        }
        if (filterList.getItemCount() > 0) {
            filterList.setSelection(0);
        }
        filterList.redraw();
    }

    public void store() {
        properties.setRegexpFilters(regexps);

        ContextStore newReg = properties.getContextRegistry();
        ContextSet newFilter = properties.getContextFilter();

        for (RegexpEntry regexpEntry : regexps) {
            // Always enable the custom context
            NamedContext newContext = newReg.getContext(regexpEntry.getName());
            if (newContext != null) {
                newFilter = newFilter.set(newContext.getIndex());
            }
            regexpEntry.setChanged(false);
        }
        properties.setContextFilter(newFilter);

        refreshList();
        regexpsLengthSinceLastStore = regexps.size();
    }

    public boolean isRebuildRequired() {
        boolean changed = regexpsLengthSinceLastStore != regexps.size();
        if (!changed) {
            for (final RegexpEntry regexp : regexps) {
                changed = regexp.isChanged();
                if (changed) {
                    break;
                }
            }
        }
        return changed;
    }

    private void doSelection() {
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (filterList.getSelectionCount() == 0) {
            editFilter.setEnabled(false);
            removeFilter.setEnabled(false);
            copyFilter.setEnabled(false);
        } else {
            editFilter.setEnabled(true);
            removeFilter.setEnabled(true);
            copyFilter.setEnabled(true);
        }
    }

    private void doAdd() {
        // add a new default regexp to
        RegexpEntry newCtx =
            new RegexpEntry(suggestNewName(regexps), "", RegexpEntry.METHOD_TYPE);

        RegexpFilterEditDialog dialog = new RegexpFilterEditDialog(getShell(), regexps);
        dialog.setData(newCtx);

        int result = dialog.open();
        if (result != Dialog.CANCEL) {
            newCtx = dialog.getData();
            regexps.add(newCtx);

            filterList.add(newCtx.getName() + (newCtx.isChanged()? " (*)" : ""), filterList.getItemCount());
            filterList.setSelection(filterList.getItemCount() - 1);

            updateButtonStates();
        }
    }

    private void doEdit() {
        if (!isItemSelected()) {
            return;
        }

        // popup the regexp edit dialog.
        int index = filterList.getSelectionIndex();
        RegexpEntry data = regexps.get(index);

        RegexpFilterEditDialog dialog = new RegexpFilterEditDialog(getShell(), regexps);
        dialog.setData(data);

        int result = dialog.open();
        if (result == Dialog.CANCEL) {
            // do not update the item.
            return;
        }

        RegexpEntry newData = dialog.getData();
        newData.setChanged(data.isChanged());

        // check if the data has changed. if so, make the appropriate updates.
        if (!newData.equivalent(data)) {
            newData.setChanged(true);
        }
        regexps.set(index, newData);
        filterList.setItem(index, newData.getName()  + (newData.isChanged()? " (*)" : ""));
    }

    private void doRemove() {
        if (!isItemSelected()) {
            return;
        }

        // prompt - are you sure you want to remove this filter.?

        int index = filterList.getSelectionIndex();
        filterList.remove(index);
        regexps.remove(index);

        // maintain the item selection.
        if (filterList.getItemCount() == 0) {
            updateButtonStates();
            return;
        }

        if (index < filterList.getItemCount()) {
            filterList.setSelection(index);
        } else {
            filterList.setSelection(filterList.getItemCount() - 1);
        }
        updateButtonStates();
    }

    private void doCopy() {
        if (!isItemSelected()) {
            return;
        }

        // copy the current selection
        int index = filterList.getSelectionIndex();
        RegexpEntry original = regexps.get(index);

        RegexpEntry copy = original.duplicate(suggestCopyName(original, regexps));
        regexps.add(copy);

        filterList.add(copy.getName()  + (copy.isChanged()? " (*)" : ""), filterList.getItemCount());

        // update selection to the newly created item.
        filterList.setSelection(filterList.getItemCount() - 1);
    }

    private boolean isItemSelected() {
        return filterList.getSelectionIndex() != -1;
    }

    /**
     * Suggest a name to be used as the default for a new regexp context.
     *
     * @param existing existing entries to avoid duplication
     * @return suggested name for new entry
     */
    private static String suggestNewName(java.util.List<RegexpEntry> existing) {
        return suggestName("Unnamed_{0}", existing);
    }

    /**
     * Suggest a name to be used as the default for a copy of the specified
     * regexp context.
     *
     * @param ctx regexp entry to suggest name for
     * @param existing existing entries to avoid duplication
     * @return suggested name for a copy of ctx
     */
    private static String suggestCopyName(RegexpEntry ctx, java.util.List<RegexpEntry> existing) {
        return suggestName("Copy_{0}_of_" + ctx.getName(), existing);
    }

    private static String suggestName(String template, java.util.List<RegexpEntry> existing) {
        String regexp = MessageFormat.format(template, "(\\d+)");
        Pattern pattern = Pattern.compile(regexp);

        long maxIndex = 0;

        for (RegexpEntry regex : existing) {
            Matcher matcher = pattern.matcher(regex.getName());
            if (matcher.matches()) {
                long index = Long.parseLong(matcher.group(1));
                if (index > maxIndex) {
                    maxIndex = index;
                }
            }
        }
        return MessageFormat.format(template, maxIndex + 1);
    }
}
