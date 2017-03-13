package com.atlassian.clover.eclipse.core.ui.widgets;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import com.atlassian.clover.eclipse.core.projects.settings.ProjectSettings;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.settings.InstallationSettings;

public class ContextFilterModificationWidget extends Composite {
    private BuiltinContextFilterSelectionWidget blockFilter;
    private RegexContextFilterModificationWidget regexFilter;
    private ProjectSettings properties;

    public ContextFilterModificationWidget(Composite composite, ProjectSettings properties) {
        super(composite, SWT.NONE);
        this.properties = properties;
        setLayout(new GridLayout());

        Group blockContexts = new Group(this, SWT.NONE);
        blockContexts.setText("Standard Coverage Contexts Filters");
        blockContexts.setLayoutData(new GridData(GridData.FILL_BOTH));
        blockContexts.setLayout(new GridLayout());

        blockFilter = new BuiltinContextFilterSelectionWidget(blockContexts, properties.getContextRegistry(), properties.getContextFilter());
        blockFilter.buildContents();
        blockFilter.updateSelection();

        blockFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Group regexContexts = new Group(this, SWT.NONE);
        regexContexts.setText("Custom Coverage Context Filters");
        regexContexts.setLayoutData(new GridData(GridData.FILL_BOTH));
        regexContexts.setLayout(new GridLayout());

        regexFilter = new RegexContextFilterModificationWidget(regexContexts, properties);
        regexFilter.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    public void store() {
        regexFilter.store();
        blockFilter.storeTo(properties);
    }

    public boolean isRebuildRequired() {
        return regexFilter.isRebuildRequired();
    }

    public boolean alertIfManualRebuildRequired() {
        if (isRebuildRequired()) {
            final InstallationSettings installationSettings = CloverPlugin.getInstance().getInstallationSettings();
            if (installationSettings.isPromptingOnContextChange()) {
                MessageDialogWithCheckbox.Result result = new MessageDialogWithCheckbox.Result();
                MessageDialogWithCheckbox.openQuestion(
                    null,
                    "Custom Coverage Context Filters",
                    "You have changed one or more regular expression context filters. A clean rebuild\n"
                        + "of your project is required for these changes to take effect. Rebuild now?",
                    true,
                    "Display this prompt again?", true,
                    result);

                installationSettings.setLatestPromptOnContextChange(result.isYesSelected());
                installationSettings.setPromptingOnContextChange(result.isChecked());
                return result.isYesSelected();
            } else {
                return installationSettings.isRebuildingOnContextChange();
            }
        } else {
            return false;
        }
    }
}
