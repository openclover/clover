package org.openclover.eclipse.testopt.settings;

import org.openclover.eclipse.testopt.TestOptimizationPlugin;
import org.openclover.eclipse.testopt.TestOptimizationPluginMessages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;

public class OptimizedLaunchConfigurationTab extends AbstractLaunchConfigurationTab {
    private final IPreferenceStore preferenceStore = new PreferenceStore();
    private TestOptimizationPreferencesControl control;
    private Button useDefaultsButton;

    @Override
    public String getName() {
        return "Test Optimization";
    }

    @Override
    public Image getImage() {
        return TestOptimizationPlugin.getDefault().getTestOptimizationIcon();
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout());

        Composite defaultsComposite = new Composite(container, SWT.NONE);
        defaultsComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        defaultsComposite.setLayout(new GridLayout(2, false));

        useDefaultsButton = new Button(defaultsComposite, SWT.CHECK);
        useDefaultsButton.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        useDefaultsButton.setText(TestOptimizationPluginMessages.getString("launch.optimized.prefs.usedefaults"));
        useDefaultsButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                control.setEnabled(!useDefaultsButton.getSelection());
                updateLaunchConfigurationDialog();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        Button copyDefaults = new Button(defaultsComposite, SWT.PUSH);
        copyDefaults.setText(TestOptimizationPluginMessages.getString("launch.optimized.prefs.copydefaults"));
        copyDefaults.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                control.loadDefaults();
            }
        });

        control = new TestOptimizationPreferencesControl(container, null, preferenceStore);
        control.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        control.setPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (FieldEditor.IS_VALID.equals(event.getProperty())) {
                    if (Boolean.TRUE.equals(event.getNewValue())) {
                        setErrorMessage(null);
                    } else {
                        final StringFieldEditor fieldEditor = (StringFieldEditor) event.getSource();
                        setErrorMessage(fieldEditor.getErrorMessage());
                    }
                }
                updateLaunchConfigurationDialog();
            }
        });
        setControl(container);
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        IPreferenceStore defaults = TestOptimizationPlugin.getDefault().getPreferenceStore();

        configuration.setAttribute(TestOptimizationPlugin.USE_DEFAULT_SETTINGS, true);
        configuration.setAttribute(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG, defaults.getBoolean(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG));
        configuration.setAttribute(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS, defaults.getBoolean(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS));
        configuration.setAttribute(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE, defaults.getInt(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE));
        configuration.setAttribute(TestOptimizationPlugin.MINIMIZE_TESTS, defaults.getBoolean(TestOptimizationPlugin.MINIMIZE_TESTS));
        configuration.setAttribute(TestOptimizationPlugin.TEST_REORDERING, defaults.getString(TestOptimizationPlugin.TEST_REORDERING));
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        IPreferenceStore defaults = TestOptimizationPlugin.getDefault().getPreferenceStore();

        copyBooleanToPreferences(configuration, defaults, TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG);
        copyBooleanToPreferences(configuration, defaults, TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS);
        copyIntToPreferences(configuration, defaults, TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE);
        copyBooleanToPreferences(configuration, defaults, TestOptimizationPlugin.MINIMIZE_TESTS);
        copyStringToPreferences(configuration, defaults, TestOptimizationPlugin.TEST_REORDERING);
        control.load();
        try {
            final boolean useDefaults = configuration.getAttribute(TestOptimizationPlugin.USE_DEFAULT_SETTINGS, true);
            useDefaultsButton.setSelection(useDefaults);
            control.setEnabled(!useDefaults);
        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute", e);
        }
    }

    @Override
    public boolean canSave() {
        return control.isValid();
    }

    private void copyBooleanToPreferences(ILaunchConfiguration config, IPreferenceStore defaults, String attribute) {
        try {
            final boolean def = defaults.getBoolean(attribute);
            preferenceStore.setDefault(attribute, def);
            preferenceStore.setValue(attribute, config.getAttribute(attribute, def));
        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute " + attribute, e);
        }
    }

    private void copyIntToPreferences(ILaunchConfiguration config, IPreferenceStore defaults, String attribute) {
        try {
            final int def = defaults.getInt(attribute);
            preferenceStore.setDefault(attribute, def);
            preferenceStore.setValue(attribute, config.getAttribute(attribute, def));
        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute " + attribute, e);
        }
    }

    private void copyStringToPreferences(ILaunchConfiguration config, IPreferenceStore defaults, String attribute) {
        try {
            final String def = defaults.getString(attribute);
            preferenceStore.setDefault(attribute, def);
            preferenceStore.setValue(attribute, config.getAttribute(attribute, def));
        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute " + attribute, e);
        }
    }



    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        control.store();
        configuration.setAttribute(TestOptimizationPlugin.USE_DEFAULT_SETTINGS, useDefaultsButton.getSelection());
        configuration.setAttribute(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG, preferenceStore.getBoolean(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG));
        configuration.setAttribute(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS, preferenceStore.getBoolean(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS));
        configuration.setAttribute(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE, preferenceStore.getInt(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE));
        configuration.setAttribute(TestOptimizationPlugin.MINIMIZE_TESTS, preferenceStore.getBoolean(TestOptimizationPlugin.MINIMIZE_TESTS));
        configuration.setAttribute(TestOptimizationPlugin.TEST_REORDERING, preferenceStore.getString(TestOptimizationPlugin.TEST_REORDERING));
    }

}
