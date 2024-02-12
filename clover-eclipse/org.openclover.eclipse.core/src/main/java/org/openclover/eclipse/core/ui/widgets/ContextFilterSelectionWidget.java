package org.openclover.eclipse.core.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.context.ContextStore;
import org.openclover.core.context.NamedContext;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;

import java.util.HashMap;
import java.util.List;

import static org.openclover.core.util.Maps.newHashMap;

public class ContextFilterSelectionWidget extends BuiltinContextFilterSelectionWidget {
    protected HashMap<String, Button> customCheckboxes;

    public ContextFilterSelectionWidget(Composite parent, ContextStore contextStore, ContextSet filter) {
        super(parent, contextStore, filter);
        customCheckboxes = newHashMap();
    }

    @Override
    public void buildContents() {
        super.buildContents();

        if (contextStore.getAllUserContexts().size() > 0) {
            Label blockLabel = new Label(this, SWT.LEFT);
            blockLabel.setText(CloverEclipsePluginMessages.REGEXP_CONTEXT_FILTER_SELECTION_INSTRUCTIONS());

            Composite container = new Composite(this, SWT.NONE);
            container.setLayout(new GridLayout(CONTEXTS_PER_LINE, true));

            List<NamedContext> customContexts = contextStore.getAllUserContexts();

            for (NamedContext context : customContexts) {
                Button check = new Button(container, SWT.CHECK);
                check.setText(context.getName());
                //Initialise all checkboxes as true, to be updated later as appropriate
                //for context set
                check.setSelection(false);
                this.customCheckboxes.put(context.getName(), check);
            }
        }
    }

    @Override
    public void updateSelection() {
        super.updateSelection();

        NamedContext[] namedContexts = contextStore.getContexts(filter);
        for (NamedContext namedContext : namedContexts) {
            Button checkbox = customCheckboxes.get(namedContext.getName());
            if (checkbox != null) {
                //Named context only provided for contexts that are set, ie false
                checkbox.setSelection(true);
            }
        }
    }

    @Override
    public ContextSet getFilterFromSelection(ContextSet filter) {
        super.getFilterFromSelection(filter);

        List<NamedContext> customContext = contextStore.getAllUserContexts();
        for (NamedContext namedContext : customContext) {
            Button checkbox = this.customCheckboxes.get(namedContext.getName());
            if (checkbox != null) {
                filter = filter.set(namedContext.getIndex(), checkbox.getSelection());
            }
        }
        return filter;
    }
}
