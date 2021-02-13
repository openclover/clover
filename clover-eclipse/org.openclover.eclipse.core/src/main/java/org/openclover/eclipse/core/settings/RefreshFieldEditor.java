package com.atlassian.clover.eclipse.core.settings;

import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import com.atlassian.clover.eclipse.core.ui.SwtUtils;

public class RefreshFieldEditor extends FieldEditor {

    private Button changeControl;
    private Combo refreshIntervalCombo;

    public RefreshFieldEditor(Composite parent) {
        super(
            getCompositePreferenceName(),
            CloverEclipsePluginMessages.AUTO_REFRESH_COVERAGE_DATA(),
            parent);

        init(
            getCompositePreferenceName(),
            CloverEclipsePluginMessages.AUTO_REFRESH_COVERAGE_DATA());
        createControl(parent);
    }

    private int getComboSelection(long refreshPeriod) {
        switch ((int) refreshPeriod) {
            case(int) InstallationSettings.Values.TWO_SECONDS_COVERAGE_REFRESH_PERIOD:
                return 0;
            case(int) InstallationSettings.Values.FIVE_SECONDS_COVERAGE_REFRESH_PERIOD:
                return 1;
            case(int) InstallationSettings.Values.TEN_SECONDS_COVERAGE_REFRESH_PERIOD:
                return 2;
            case(int) InstallationSettings.Values.TWENTY_SECONDS_COVERAGE_REFRESH_PERIOD:
                return 3;
            default:
                return getDefaultComboSelection();
        }
    }

    private int getDefaultComboSelection() {
        return getComboSelection(InstallationSettings.Defaults.COVERAGE_REFRESH_PERIOD);
    }

    private long getPreferenceValueForComboSelection() {
        switch (refreshIntervalCombo.getSelectionIndex()) {
            case 0:
                return InstallationSettings.Values.TWO_SECONDS_COVERAGE_REFRESH_PERIOD;
            case 1:
                return InstallationSettings.Values.FIVE_SECONDS_COVERAGE_REFRESH_PERIOD;
            case 2:
                return InstallationSettings.Values.TEN_SECONDS_COVERAGE_REFRESH_PERIOD;
            case 3:
                return InstallationSettings.Values.TWENTY_SECONDS_COVERAGE_REFRESH_PERIOD;
            default:
                return InstallationSettings.Defaults.COVERAGE_REFRESH_PERIOD;
        }
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        SwtUtils.setHorizontalSpan(refreshIntervalCombo, numColumns - 1);
    }

    @Override
    protected void doLoad() {
        changeControl.setSelection(getPreferenceStore().getBoolean(InstallationSettings.Keys.AUTO_REFRESH_COVERAGE_DATA));
        refreshIntervalCombo.select(
            getComboSelection(getPreferenceStore().getLong(InstallationSettings.Keys.COVERAGE_REFRESH_PERIOD)));
    }

    @Override
    protected void doLoadDefault() {
        changeControl.setSelection(InstallationSettings.Defaults.REFRESH_COVERAGE);
        refreshIntervalCombo.select(getDefaultComboSelection());
    }

    @Override
    protected void doStore() {
        getPreferenceStore().setValue(
            InstallationSettings.Keys.AUTO_REFRESH_COVERAGE_DATA,
            changeControl.getSelection());
        getPreferenceStore().setValue(
            InstallationSettings.Keys.COVERAGE_REFRESH_PERIOD,
            getPreferenceValueForComboSelection());
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        if (changeControl == null) {
            changeControl = new Button(parent, SWT.CHECK);
            changeControl.setLayoutData(new GridData());
            changeControl.setText(getLabelText());

            refreshIntervalCombo = new Combo(parent, SWT.READ_ONLY);
            refreshIntervalCombo.setItems(new String[]{
                CloverEclipsePluginMessages.TWO_SECONDS(),
                CloverEclipsePluginMessages.FIVE_SECONDS(),
                CloverEclipsePluginMessages.TEN_SECONDS(),
                CloverEclipsePluginMessages.TWENTY_SECONDS()
            });

            SwtUtils.setHorizontalSpan(refreshIntervalCombo, numColumns - 1);
        } else {
            checkParent(changeControl, parent);
            checkParent(refreshIntervalCombo, parent);
        }
    }

    public static String getCompositePreferenceName() {
        return "composite[" + InstallationSettings.Keys.AUTO_REFRESH_COVERAGE_DATA + "," + InstallationSettings.Keys.COVERAGE_REFRESH_PERIOD + "]";
    }
}
