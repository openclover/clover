package org.openclover.eclipse.testopt.settings;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openclover.eclipse.testopt.TestOptimizationPlugin;

public class TestOptimizationGlobalPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
    private TestOptimizationPreferencesControl control;

    @Override
    protected Control createContents(Composite parent) {
        control = new TestOptimizationPreferencesControl(parent, this, getPreferenceStore());
        return control;
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(TestOptimizationPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void performDefaults() {
        control.loadDefaults();
    }

    @Override
    public boolean performOk() {
        control.store();
        return super.performOk();
    }

}
