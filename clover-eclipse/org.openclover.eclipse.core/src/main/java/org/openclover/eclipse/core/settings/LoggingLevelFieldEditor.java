package org.openclover.eclipse.core.settings;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.openclover.eclipse.core.ui.SwtUtils;

public class LoggingLevelFieldEditor extends FieldEditor {
    private Combo combo;

    /**
     * Enumeration with list of logging levels used in combo box. Performs also
     * mapping to/from logging level value from InstallationSettings class.
     * Enum ordinal = index in combo box. Enum name = title.
     */
    enum LoggingLevelCombo {
        None(InstallationSettings.Values.NO_LOGGING_LEVEL),
        Verbose(InstallationSettings.Values.VERBOSE_LOGGING_LEVEL),
        Debug(InstallationSettings.Values.DEBUG_LOGGING_LEVEL),
        Info(InstallationSettings.Values.INFO_LOGGING_LEVEL);

        /**
         * Value as used in InstallationSettings
         * @see InstallationSettings
         */
        private String installationValue;

        /**
         * Associate enum with matching value from InstallationSettings class.
         * @param installationValue
         */
        LoggingLevelCombo(String installationValue) {
            this.installationValue = installationValue;
        }

        /**
         * Converts logging level from installation settings into appropriate index in the combo box.
         * @param value
         * @return int - index
         */
        static int fromInstallationSetting(String value) {
            for (LoggingLevelCombo level : values()) {
                if ((level.installationValue != null) && (level.installationValue.equals(value))) {
                    return level.ordinal();
                }
            }
            return None.ordinal();
        }

        /**
         * Converts combo box index into logging value for installation settings.
         * @param index
         * @return String - named value
         */
        static String toInstallationSetting(int index) {
            for (LoggingLevelCombo level : values()) {
                if (level.ordinal() == index) {
                    return level.installationValue;
                }
            }
            return None.installationValue;
        }

        /**
         * Returns all enum names as array of Strings
         * @return String[] - enum.toString() for all enums
         */
        static String[] stringValues() {
            String[] values = new String[values().length];
            int i = 0;
            for (LoggingLevelCombo level : values()) {
                values[i++] = level.toString();
            }
            return values;
        }
    }


    public LoggingLevelFieldEditor(Composite parent) {
        super(
            InstallationSettings.Keys.LOGGING_LEVEL,
            "Clover Plugin logging output level:",
            parent);
    }

    public void doSelect(LoggingLevelCombo level) {
        combo.select(level.ordinal());
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        SwtUtils.setHorizontalSpan(combo, numColumns - 1);
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        if (combo == null) {
            getLabelControl(parent);
            combo = new Combo(parent, SWT.READ_ONLY);
            combo.setItems(LoggingLevelCombo.stringValues());
        } else {
            checkParent(combo, parent);
        }
    }

    @Override
    protected void doLoad() {
        combo.select(
            getComboSelection(getPreferenceStore().getString(InstallationSettings.Keys.LOGGING_LEVEL)));
    }

    @Override
    protected void doLoadDefault() {
        combo.select(LoggingLevelCombo.None.ordinal());
    }

    @Override
    protected void doStore() {
        getPreferenceStore().setValue(
            InstallationSettings.Keys.LOGGING_LEVEL,
            getPreferenceValueForComboSelection());
    }

    private int getComboSelection(String value) {
        return LoggingLevelCombo.fromInstallationSetting(value);
    }

    private String getPreferenceValueForComboSelection() {
        return LoggingLevelCombo.toInstallationSetting(combo.getSelectionIndex());
    }
}
