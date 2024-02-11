package org.openclover.eclipse.testopt.settings;

import org.openclover.core.api.optimization.OptimizationOptions;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.testopt.TestOptimizationPlugin;
import org.openclover.eclipse.testopt.TestOptimizationPluginMessages;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.Collection;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newHashMap;

public class TestOptimizationPreferencesControl extends Composite {
    private final BooleanFieldEditor showNoTestsEditor;
    private BooleanFieldEditor discardSnapshotEditor;
    private IntegerFieldEditor discardSnapshotAgeEditor;
    private final BooleanFieldEditor minimizeTestsEditor;
    private final RadioGroupFieldEditor testReorderingEditor;

    private final Collection<FieldEditor> allEditors = newArrayList();
    private final Map<FieldEditor, Composite> parents = newHashMap();

    private static final String[][] TEST_REORDERING_OPTIONS = new String[][]{
            {TestOptimizationPluginMessages.getString("launch.optimized.prefs.testsreordering.none"), OptimizationOptions.TestSortOrder.NONE.name()},
            {TestOptimizationPluginMessages.getString("launch.optimized.prefs.testsreordering.failfast"), OptimizationOptions.TestSortOrder.FAILFAST.name()},
            {TestOptimizationPluginMessages.getString("launch.optimized.prefs.testsreordering.random"), OptimizationOptions.TestSortOrder.RANDOM.name()},
    };
    private Composite discardAgeParent;
    private Label compilesLabel;


    public TestOptimizationPreferencesControl(Composite parent, DialogPage page, IPreferenceStore preferenceStore) {
        super(parent, SWT.NULL);

        setLayout(new GridLayout());

        final Composite snteComposite = new Composite(this, SWT.NONE);
        showNoTestsEditor = addEditor(new BooleanFieldEditor(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG,
                                                   TestOptimizationPluginMessages.getString("launch.optimized.prefs.shownotestsfound"),
                                                   snteComposite), snteComposite);
        createDiscardPanel();
        final Composite mteComposite = new Composite(this, SWT.NONE);
        minimizeTestsEditor = addEditor(new BooleanFieldEditor(TestOptimizationPlugin.MINIMIZE_TESTS,
                                                   TestOptimizationPluginMessages.getString("launch.optimized.prefs.testsminimize"),
                                                   mteComposite), mteComposite);

        final Composite testReorderingParent = new Composite(this, SWT.NONE);
        final GridData layoutData1 = new GridData(GridData.FILL_HORIZONTAL);
        testReorderingParent.setLayoutData(layoutData1);
        testReorderingEditor = addEditor(new RadioGroupFieldEditor(TestOptimizationPlugin.TEST_REORDERING,
                                                         TestOptimizationPluginMessages.getString("launch.optimized.prefs.testsreordering"),
                                                         1, TEST_REORDERING_OPTIONS, testReorderingParent, true), testReorderingParent);

        for (FieldEditor editor : allEditors) {
            editor.setPage(page);
            editor.setPreferenceStore(preferenceStore);
        }

        load();
    }

    private void createDiscardPanel() {
        Composite composite = new Composite(this, SWT.NONE);

        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;

        composite.setLayoutData(data);
        composite.setLayout(new GLH(3, false).marginWidth(0).marginHeight(0).getGridLayout());

        Composite dseComposite = new Composite(composite, SWT.NONE);
        discardSnapshotEditor = addEditor(new BooleanFieldEditor(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS,
                                                       TestOptimizationPluginMessages.getString("launch.optimized.prefs.dicardstale"),
                                                       dseComposite), dseComposite);
        discardAgeParent = new Composite(composite, SWT.NONE);
        discardAgeParent.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        discardSnapshotAgeEditor = addEditor(new IntegerFieldEditor(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE, "", discardAgeParent, 6), discardAgeParent);
        discardSnapshotAgeEditor.setValidRange(2, 10000);

        compilesLabel = new Label(composite, SWT.NONE);
        compilesLabel.setText("compiles");
        compilesLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

        discardSnapshotEditor.setPropertyChangeListener(discardSnapshotAgeEnabler);
    }

    private final IPropertyChangeListener discardSnapshotAgeEnabler = new IPropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            discardSnapshotAgeEditor.setEnabled(isEnabled() && discardSnapshotEditor.getBooleanValue(), discardAgeParent);
        }
    };
    protected void loadDefaults() {
        for (FieldEditor editor : allEditors) {
            editor.loadDefault();
        }
        discardSnapshotAgeEnabler.propertyChange(null);
    }

    public void load() {
        for (FieldEditor editor : allEditors) {
            editor.load();
        }
        discardSnapshotAgeEnabler.propertyChange(null);
    }

    public void store() {
        for (FieldEditor editor : allEditors) {
            if (editor.isValid()) {
                editor.store();
            }
        }
    }

    public void setPropertyChangeListener(final IPropertyChangeListener listener) {
        for (FieldEditor editor : allEditors) {
            if (editor == discardSnapshotEditor) {
                discardSnapshotEditor.setPropertyChangeListener(listener == null
                        ? discardSnapshotAgeEnabler
                        : (IPropertyChangeListener) event -> {
                            discardSnapshotAgeEnabler.propertyChange(event);
                            listener.propertyChange(event);
                        });
            } else {
                editor.setPropertyChangeListener(listener);
            }
        }
    }

    public boolean isValid() {
        for (FieldEditor editor : allEditors) {
            if (!editor.isValid()) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (FieldEditor editor : allEditors) {
            editor.setEnabled(enabled, getParent(editor));
        }
        compilesLabel.setEnabled(enabled);
        testReorderingEditor.getRadioBoxControl(getParent(testReorderingEditor)).setEnabled(enabled);

        if (enabled) {
            discardSnapshotAgeEnabler.propertyChange(null);
        }
    }

    private <T extends FieldEditor> T addEditor(T editor, Composite parent) {
        allEditors.add(editor);
        parents.put(editor, parent);
        return editor;
    }

    private Composite getParent(FieldEditor editor) {
        return parents.get(editor);
    }
}
