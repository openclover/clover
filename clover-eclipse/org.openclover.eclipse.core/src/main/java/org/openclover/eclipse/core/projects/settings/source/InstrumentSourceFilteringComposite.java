package org.openclover.eclipse.core.projects.settings.source;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.projects.settings.ProjectPropertyPage;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.eclipse.core.settings.InstallationSettings;
import org.openclover.eclipse.core.ui.widgets.MessageDialogWithCheckbox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class InstrumentSourceFilteringComposite extends Composite {
    private final Button filterFoldersAllButton;
    private final Button filterFoldersSelectedButton;
    private final InstrumentSourcePatternsComponent instrumentSourcePatternsComponent;
    private final Text includeFilterText;
    private final Text excludeFilterText;

    private String lastInclude;
    private String lastExclude;
    private boolean lastInstrumentSelectedSourceFolders;

    public InstrumentSourceFilteringComposite(Composite parent, ProjectSettings properties, SourceRootsWithPatternTreeContentProvider contentProvider) {
        super(parent, SWT.NONE);

        lastInclude = properties.getIncludeFilter() == null ? ProjectSettings.DEFAULT_INCLUDE_PATTERN : properties.getIncludeFilter();
        lastExclude = properties.getExcludeFilter() == null ? ProjectSettings.DEFAULT_EXCLUDE_PATTERN : properties.getExcludeFilter();
        lastInstrumentSelectedSourceFolders = properties.isInstrumentSelectedSourceFolders();

        final Group filteringGroup = LayoutUtils.createGroup(this, CloverEclipsePluginMessages.FILE_FILTERING());
        filteringGroup.setLayout(new GLH().standardiseMargin().getGridLayout());
        filteringGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        filterFoldersAllButton = new Button(filteringGroup, SWT.RADIO);
        filterFoldersAllButton.setText(CloverEclipsePluginMessages.FILE_FILTERING_ALL_FOLDERS());

        final Composite c = new Composite(filteringGroup, SWT.NONE);
        c.setLayout(new GridLayout(2, false));
        final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = ProjectPropertyPage.Panel.HORIZONTAL_INDENT;
        c.setLayoutData(gd);

        final Label includeLabel = new Label(c, SWT.NONE);
        includeLabel.setText(CloverEclipsePluginMessages.FILE_FILTERING_INCLUDE());

        includeFilterText = new Text(c, SWT.BORDER);
        includeFilterText.setText(lastInclude);
        includeFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Label excludeLabel = new Label(c, SWT.NONE);
        excludeLabel.setText(CloverEclipsePluginMessages.FILE_FILTERING_EXCLUDE());

        excludeFilterText = new Text(c, SWT.BORDER);
        excludeFilterText.setText(lastExclude);
        excludeFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        filterFoldersSelectedButton = new Button(filteringGroup, SWT.RADIO);
        filterFoldersSelectedButton.setText(CloverEclipsePluginMessages.FILE_FILTERING_SELECTED_FOLDERS());

        instrumentSourcePatternsComponent = new InstrumentSourcePatternsComponent(filteringGroup, contentProvider);
        final GridData gd1 = SwtUtils.gridDataFor(instrumentSourcePatternsComponent);
        gd1.horizontalIndent = ProjectPropertyPage.Panel.HORIZONTAL_INDENT;
        gd1.minimumHeight = 100;
        gd1.grabExcessVerticalSpace = true;

        if (lastInstrumentSelectedSourceFolders) {
            filterFoldersSelectedButton.setSelection(true);
        } else {
            filterFoldersAllButton.setSelection(true);
        }

        final SelectionAdapter adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                final boolean enableAllPart = filterFoldersAllButton.getSelection();
                includeLabel.setEnabled(enableAllPart);
                excludeLabel.setEnabled(enableAllPart);
                includeFilterText.setEnabled(enableAllPart);
                excludeFilterText.setEnabled(enableAllPart);

                instrumentSourcePatternsComponent.setEnabled(!enableAllPart);
            }
        };
        filterFoldersAllButton.addSelectionListener(adapter);
        adapter.widgetSelected(null); // update enabled state
    }

    public void storeTo(ProjectSettings settings) {
        lastInstrumentSelectedSourceFolders = filterFoldersSelectedButton.getSelection();
        settings.setInstrumentSelectedSourceFolders(lastInstrumentSelectedSourceFolders);

        lastInclude = includeFilterText.getText().trim();
        settings.setIncludeFilter(lastInclude.length() > 0 ? lastInclude : null);

        lastExclude = excludeFilterText.getText().trim();
        settings.setExcludeFilter(lastExclude.length() > 0 ? lastExclude : null);

        instrumentSourcePatternsComponent.storeTo(settings);
    }

    public boolean isModified() {
        if (filterFoldersSelectedButton.getSelection() != lastInstrumentSelectedSourceFolders) {
            return true;
        }

        if (filterFoldersSelectedButton.getSelection()) {
            return instrumentSourcePatternsComponent.isModified();
        } else {
            return !includeFilterText.getText().trim().equals(lastInclude)
                    || !excludeFilterText.getText().trim().equals(lastExclude);
        }
    }

    public static boolean askForRebuild() {
        final InstallationSettings installationSettings = CloverPlugin.getInstance().getInstallationSettings();
        if (installationSettings.isPromptingOnInstrumentationSourceChange()) {
            final MessageDialogWithCheckbox.Result result = new MessageDialogWithCheckbox.Result();
            MessageDialogWithCheckbox.openQuestion(
                    null,
                    CloverEclipsePluginMessages.INSTRUMENTATION_SOURCE_CHANGED_TITLE(),
                    CloverEclipsePluginMessages.INSTRUMENTATION_SOURCE_CHANGED_QUESTION(),
                    true,
                    CloverEclipsePluginMessages.DISPLAY_THIS_PROMPT_AGAIN(), true,
                    result);

            installationSettings.setLatestPromptOnInstrumentationSourceChange(result.isYesSelected());
            installationSettings.isPromptingOnInstrumentationSourceChange(result.isChecked());
            return result.isYesSelected();
        } else {
            return installationSettings.isRebuildingOnInstrumentationSourceChange();
        }
    }
}
