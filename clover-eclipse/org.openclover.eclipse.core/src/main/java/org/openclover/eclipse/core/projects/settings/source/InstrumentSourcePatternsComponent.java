package org.openclover.eclipse.core.projects.settings.source;

import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InstrumentSourcePatternsComponent extends Composite {
    private final CheckboxTableViewer foldersListViewer;
    private final Button editBtn;

    private List<SourceFolderPattern> lastPatterns;
    private final SourceRootsWithPatternTreeContentProvider contentProvider;

    public InstrumentSourcePatternsComponent(Composite parent, final SourceRootsWithPatternTreeContentProvider contentProvider) {
        super(parent, SWT.NONE);
        this.contentProvider = contentProvider;
        lastPatterns = getSFPList(contentProvider);

        setLayout(new GridLayout(2, false));

        foldersListViewer = CheckboxTableViewer.newCheckList(this, SWT.BORDER | SWT.V_SCROLL);
        foldersListViewer.setLabelProvider(new FolderWithPatternLabelProvider());
        foldersListViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                final SourceRootWithPattern element = (SourceRootWithPattern) event.getElement();
                element.getPattern().setEnabled(event.getChecked());
                contentProvider.notifyElementChange(element);
            }
        });

        foldersListViewer.setContentProvider(contentProvider);
        foldersListViewer.setInput(contentProvider.getProject());
        foldersListViewer.setCheckedElements(contentProvider.getEnabledElements());

        GridData gd = SwtUtils.gridDataFor(foldersListViewer.getControl());
        gd.verticalSpan = 2;
        gd.grabExcessVerticalSpace = true;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.FILL;
        gd.minimumWidth = 512;

        editBtn = new Button(this, SWT.NONE);
        editBtn.setText("Edit");
        editBtn.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        editBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doEdit();
            }
        });

        new Label(this, SWT.NONE);

        foldersListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateButtonStateAndUpdateListeners();
            }
        });
    }

    private void updateButtonStateAndUpdateListeners() {
        final boolean enabled = isEnabled();

        editBtn.setEnabled(enabled && !foldersListViewer.getSelection().isEmpty());
        contentProvider.notifyPerFolderSettingsEnabled(enabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        foldersListViewer.getControl().setEnabled(enabled);

        updateButtonStateAndUpdateListeners();
    }

    private void doEdit() {
        StructuredSelection selection = (StructuredSelection) foldersListViewer.getSelection();
        SourceRootWithPattern srwp = (SourceRootWithPattern) selection.getFirstElement();
        if (srwp != null) {
            EditSourceFolderPatternDialog dialog = new EditSourceFolderPatternDialog(getShell(), srwp.getPattern());
            if (dialog.open() == Dialog.OK) {
                SourceFolderPattern newSfp = dialog.getResult();
                srwp.setPattern(newSfp);
                foldersListViewer.refresh(srwp);
            }
        }

    }

    private static List<SourceFolderPattern> getSFPList(SourceRootsWithPatternTreeContentProvider provider) {
        final SourceRootWithPattern[] elements = provider.getAllElements();
        final List<SourceFolderPattern> patterns = new ArrayList<>(elements.length);

        for (SourceRootWithPattern srwp : elements) {
            patterns.add(new SourceFolderPattern(srwp.getPattern()));
        }
        return patterns;
    }

    public void storeTo(ProjectSettings settings) {
        lastPatterns = getSFPList(contentProvider);
        settings.setInstrumentedFolderPatterns(lastPatterns);
    }

    public boolean isModified() {
        final SourceRootWithPattern[] elements = contentProvider.getAllElements();

        Iterator<SourceFolderPattern> i = lastPatterns.iterator();
        for (SourceRootWithPattern element : elements) {
            /* array/list length mismatch here would be an error */
            if (!element.getPattern().equals(i.next())) {
                return true;
            }
        }
        return false;
    }
}
