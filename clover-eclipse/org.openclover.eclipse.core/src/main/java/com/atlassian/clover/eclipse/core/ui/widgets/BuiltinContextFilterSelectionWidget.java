package com.atlassian.clover.eclipse.core.ui.widgets;

import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.projects.settings.ProjectSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.HashMap;
import java.util.List;

import static clover.com.google.common.collect.Maps.newHashMap;

public class BuiltinContextFilterSelectionWidget extends Composite {
    protected static final int CONTEXTS_PER_LINE = 3;

    private HashMap blockCheckboxes;
    private HashMap regexCheckboxes;
    protected ContextStore contextStore;
    protected ContextSet filter;

    public BuiltinContextFilterSelectionWidget(Composite parent, ContextStore contextStore, ContextSet filter) {
        super(parent, SWT.NONE);
        setLayout(new RowLayout(SWT.VERTICAL));

        this.blockCheckboxes = newHashMap();
        this.regexCheckboxes = newHashMap();
        this.contextStore = contextStore;
        this.filter = filter;
    }

    public void buildContents() {
        createBlockContextContainerWidget();
    }

    private Composite createBlockContextContainerWidget() {
        Label blockLabel = new Label(this, SWT.LEFT);
        blockLabel.setText(CloverEclipsePluginMessages.BLOCK_CONTEXT_FILTER_INSTRUCTIONS());

        Composite container = new Composite(this, SWT.NONE);
        container.setLayout(new GridLayout(CONTEXTS_PER_LINE, true));

        List<NamedContext> reservedContexts = contextStore.getReservedContexts();
        for (NamedContext context : reservedContexts) {
            if (context.getIndex() != ContextStore.CONTEXT_CLOVER_OFF) {
                Button check = new Button(container, SWT.CHECK);
                check.setText(makeBlockContextUserFriendly(context.getName()));
                //Initialise all checkboxes as true, to be updated later as appropriate
                //for context set
                check.setSelection(false);
                blockCheckboxes.put(context.getName(), check);
            }
        }

        List<MethodRegexpContext> reservedMethodContexts = contextStore.getReservedMethodContexts();
        for (MethodRegexpContext context : reservedMethodContexts) {
            Button check = new Button(container, SWT.CHECK);
            check.setText(makeRegexContextUserFriendly(context.getName()));
            //Initialise all checkboxes as true, to be updated later as appropriate
            //for context set
            check.setSelection(false);
            regexCheckboxes.put(context.getName(), check);
        }

        return container;
    }

    private String makeBlockContextUserFriendly(String contextName) {
        return CloverEclipsePluginMessages.RESERVED_BLOCK_CONTEXT(contextName);
    }

    private String makeRegexContextUserFriendly(String contextName) {
        return CloverEclipsePluginMessages.RESERVED_REGEX_CONTEXT(contextName);
    }

    public void updateSelection() {
        NamedContext[] namedContexts = contextStore.getContexts(filter);
        for (NamedContext namedContext : namedContexts) {
            Button checkbox = (Button) blockCheckboxes.get(namedContext.getName());
            //CONTEXT_CLOVER_OFF isn't specified as a button
            if (checkbox != null) {
                //Named context only provided for contexts that are set, ie false
                checkbox.setSelection(true);
            }
        }

        for (NamedContext namedContext : namedContexts) {
            Button checkbox = (Button) regexCheckboxes.get(namedContext.getName());
            if (checkbox != null) {
                //Named context only provided for contexts that are set, ie false
                checkbox.setSelection(true);
            }
        }
    }

    public void storeTo(ProjectSettings properties) {
        properties.setContextFilter(getFilterFromSelection(properties.getContextFilter()));
    }

    public ContextSet getFilterFromSelection() {
        return getFilterFromSelection(new ContextSet());
    }

    public ContextSet getFilterFromSelection(ContextSet filter) {
        List<NamedContext> reservedContext = contextStore.getReservedContexts();
        for (NamedContext namedContext : reservedContext) {
            Button checkbox = (Button) blockCheckboxes.get(namedContext.getName());
            //CONTEXT_CLOVER_OFF isn't specified as a button
            if (checkbox != null) {
                filter = filter.set(namedContext.getIndex(), checkbox.getSelection());
            }
        }

        List<MethodRegexpContext> reservedMethodContext = contextStore.getReservedMethodContexts();
        for (MethodRegexpContext namedContext : reservedMethodContext) {
            Button checkbox = (Button) regexCheckboxes.get(namedContext.getName());
            if (checkbox != null) {
                filter = filter.set(namedContext.getIndex(), checkbox.getSelection());
            }
        }

        return filter;
    }

    public boolean filtersHaveChangedSinceLastSave() {
        return !filter.equals(getFilterFromSelection());
    }
}
