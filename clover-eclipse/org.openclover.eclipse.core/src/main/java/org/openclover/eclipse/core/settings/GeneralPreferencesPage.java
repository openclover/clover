package org.openclover.eclipse.core.settings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.core.ui.SwtUtils;

import java.io.IOException;

public class GeneralPreferencesPage
    extends BasePreferencePage {

    private Panel panel;

    @Override
    protected Control createContents(Composite parent) {
        try {
            return panel = new Panel(parent);
        } catch (CoreException e) {
            CloverPlugin.logError("Error creating general preferences panel", e);
            return null;
        }
    }

    private class Panel extends Composite {

        private RadioGroupFieldEditor rebuildActionEditor;
        private BooleanFieldEditor promptOnRebuildFieldEditor;
        private FieldEditor automaticallyRefreshEditor;
        private StringFieldEditor spanEditor;
        private LoggingLevelFieldEditor loggingLevelFiedEditor;
        private BooleanFieldEditor primeAWTInReports;
        private BooleanFieldEditor showExclusionAnnotations;
        private RadioGroupFieldEditor onContextChangeActionEditor;
        private BooleanFieldEditor promptOnContextChangeFieldEditor;
        private RadioGroupFieldEditor onInstrumentationSourceChangeActionEditor;
        private BooleanFieldEditor promptOnInstrumentationSourceChangeFieldEditor;
        private BooleanFieldEditor preserveInstrSourcesEditor;
        private BooleanFieldEditor autoOpenCloverViews;

        public Panel(Composite parent)
            throws CoreException {
            super(parent, SWT.NONE);

            setLayout(new GridLayout(1, false));

            addRebuildActionWidget();
            addPromptOnRebuildWidget();

            Group refreshGroup = new Group(this, SWT.NONE);
            refreshGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
            refreshGroup.setLayout(new GLH(2, false).marginHeight(10).marginWidth(100).getGridLayout());
            refreshGroup.setText(CloverEclipsePluginMessages.COVERAGE_DATA());

            addCoverageRefreshWidget(refreshGroup);
            addSpanWidget(refreshGroup);

            addOnContextChangeActionWidget();
            addPromptOnContextChangeWidget();

            addOnInstrumentationSourceChangeActionWidget();
            addPromptOnInstrumentationSourceContextChangeWidget();
            
            Group miscGroup = new Group(this, SWT.NONE);
            miscGroup.setLayout(new GridLayout(2, false));
            miscGroup.setText(CloverEclipsePluginMessages.MISC_PREFERENCES());

            addAWTInitialiseWidget(miscGroup);
            addShowExclusionAnnotationsWidget(miscGroup);
            addLoggingLevelWidget(miscGroup);
            addAutoOpenCloverViewsWidget(miscGroup);
            addPreserveInstrSourcesWidget(miscGroup);

            Control[] children = getChildren();
            for (Control child : children) {
                child.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
            
            bindPreferencesStoreToEditors(
                new ScopedPreferenceStore(ConfigurationScope.INSTANCE, CloverPlugin.ID),
                allConfigurationEditors()
            );

            loadEditors(allConfigurationEditors());
        }

        private void addRebuildActionWidget() {
            rebuildActionEditor = new RadioGroupFieldEditor(
                InstallationSettings.Keys.ACTION_WHEN_REBUILDING,
                CloverEclipsePluginMessages.WHEN_REBUILDING(),
                1,
                new String[][]{
                    {
                        CloverEclipsePluginMessages.CLEAR_COVERAGE(),
                        InstallationSettings.Values.WHEN_REBUILDING_CLEAR_COVERAGE
                    },
                    {
                        CloverEclipsePluginMessages.LEAVE_COVERAGE(),
                        InstallationSettings.Values.WHEN_REBUILDING_LEAVE_COVERAGE
                    },
                },
                this,
                true);
            rebuildActionEditor.setIndent(0);
        }

        private void addPromptOnRebuildWidget() {
            Composite rebuildGroup =
                (Composite)getChildren()[getChildren().length - 1];
            rebuildGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

            promptOnRebuildFieldEditor = new BooleanFieldEditor(
                InstallationSettings.Keys.WHEN_REBUILDING_PROMPT_ME,
                CloverEclipsePluginMessages.PROMPT_ME_ON_REBUILD(),
                BooleanFieldEditor.DEFAULT,
                rebuildGroup);
        }

        private void addOnContextChangeActionWidget() {
            onContextChangeActionEditor = new RadioGroupFieldEditor(
                InstallationSettings.Keys.ACTION_WHEN_CONTEXT_CHANGES,
                CloverEclipsePluginMessages.WHEN_CONTEXT_CHANGES(),
                1,
                new String[][]{
                    {
                        CloverEclipsePluginMessages.REBUILD_ON_CONTEXT_CHANGE(),
                        InstallationSettings.Values.WHEN_CONTEXT_CHANGES_REBUILD
                    },
                    {
                        CloverEclipsePluginMessages.IGNORE_ON_CONTEXT_CHANGE(),
                        InstallationSettings.Values.WHEN_CONTEXT_CHANGES_IGNORE
                    },
                },
                this,
                true);
            onContextChangeActionEditor.setIndent(0);
        }

        private void addPromptOnContextChangeWidget() {
            Composite contextChangeGroup =
                (Composite)getChildren()[getChildren().length - 1];
            contextChangeGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

            promptOnContextChangeFieldEditor = new BooleanFieldEditor(
                InstallationSettings.Keys.WHEN_CONTEXT_CHANGES_PROMPT_ME,
                CloverEclipsePluginMessages.PROMPT_ME_ON_CONTEXT_CHANGE(),
                BooleanFieldEditor.DEFAULT,
                contextChangeGroup);
        }

        private void addOnInstrumentationSourceChangeActionWidget() {
            onInstrumentationSourceChangeActionEditor = new RadioGroupFieldEditor(
                InstallationSettings.Keys.ACTION_WHEN_INSTRUMENTATION_SOURCE_CHANGES,
                CloverEclipsePluginMessages.WHEN_INSTRUMENTATION_SOURCE_CHANGES(),
                1,
                new String[][]{
                    {
                        CloverEclipsePluginMessages.REBUILD_ON_CONTEXT_CHANGE(), /* Deliberately leaving the same values */
                        InstallationSettings.Values.WHEN_CONTEXT_CHANGES_REBUILD
                    },
                    {
                        CloverEclipsePluginMessages.IGNORE_ON_CONTEXT_CHANGE(),
                        InstallationSettings.Values.WHEN_CONTEXT_CHANGES_IGNORE
                    },
                },
                this,
                true);
            onInstrumentationSourceChangeActionEditor.setIndent(0);
        }

        private void addPromptOnInstrumentationSourceContextChangeWidget() {
            Composite instrSrcChangeGroup = (Composite)getChildren()[getChildren().length - 1];
            instrSrcChangeGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

            promptOnInstrumentationSourceChangeFieldEditor = new BooleanFieldEditor(
                InstallationSettings.Keys.WHEN_INSTRUMENTATION_SOURCE_CHANGES_PROMPT_ME,
                CloverEclipsePluginMessages.PROMPT_ME_ON_CONTEXT_CHANGE(),
                BooleanFieldEditor.DEFAULT,
                instrSrcChangeGroup);
        }

        private void addCoverageRefreshWidget(Group refreshGroup) {
            automaticallyRefreshEditor = new RefreshFieldEditor(refreshGroup);
        }

        private void addSpanWidget(Group refreshGroup) {
            spanEditor = new StringFieldEditor(
                InstallationSettings.Keys.COVERAGE_SPAN, CloverEclipsePluginMessages.SPAN(), refreshGroup);

            Label label = SwtUtils.createMultilineLabel(
                refreshGroup,
                CloverEclipsePluginMessages.SPAN_BLURB(),
                convertWidthInCharsToPixels(60));
            SwtUtils.setHorizontalSpan(label, 2);
        }

        private void addAWTInitialiseWidget(Group miscGroup) {
            primeAWTInReports = new BooleanFieldEditor(
                InstallationSettings.Keys.PRIME_AWT_IN_UI_THREAD,
                CloverEclipsePluginMessages.PRIME_AWT_IN_UI_THREAD(),
                BooleanFieldEditor.DEFAULT,
                (Composite)SwtUtils.setHorizontalSpan(new Composite(miscGroup, SWT.NONE), 2));
        }

        private void addShowExclusionAnnotationsWidget(Group miscGroup) {
            showExclusionAnnotations =  new BooleanFieldEditor(
                    InstallationSettings.Keys.SHOW_EXCLUSION_ANNOTATIONS,
                    CloverEclipsePluginMessages.SHOW_EXCLUSION_ANNOTATIONS(),
                    BooleanFieldEditor.DEFAULT,
                    (Composite)SwtUtils.setHorizontalSpan(new Composite(miscGroup, SWT.NONE), 2));
        }

        private void addLoggingLevelWidget(Group miscGroup) {
            loggingLevelFiedEditor = new LoggingLevelFieldEditor(miscGroup);
        }

        private void addPreserveInstrSourcesWidget(Group miscGroup) {
            preserveInstrSourcesEditor = new BooleanFieldEditor(
                    InstallationSettings.Keys.PRESERVE_INSTRUMENTED_SOURCES,
                    CloverEclipsePluginMessages.PRESERVE_INSTRUMENTED_SOURCES(),
                    BooleanFieldEditor.DEFAULT,
                    (Composite)SwtUtils.setHorizontalSpan(new Composite(miscGroup, SWT.NONE), 2));

            preserveInstrSourcesEditor.setPropertyChangeListener(event -> {
                // automatically switch to "INFO" logging level if user enables source preserving
                // otherwise user would not see where sources were stored in Error Log view
                if ( (loggingLevelFiedEditor != null) && (event.getSource() instanceof BooleanFieldEditor) ) {
                    if (((BooleanFieldEditor) event.getSource()).getBooleanValue()) {
                        loggingLevelFiedEditor.doSelect(LoggingLevelFieldEditor.LoggingLevelCombo.Info);
                    }
                }
            });
        }

        private void addAutoOpenCloverViewsWidget(Group miscGroup) {
            autoOpenCloverViews = new BooleanFieldEditor(
                    InstallationSettings.Keys.AUTO_OPEN_CLOVER_VIEWS,
                    CloverEclipsePluginMessages.AUTO_OPEN_CLOVER_VIEWS(),
                    BooleanFieldEditor.DEFAULT,
                    (Composite)SwtUtils.setHorizontalSpan(new Composite(miscGroup, SWT.NONE), 2));
        }

        private FieldEditor[] allConfigurationEditors() {
            return new FieldEditor[] {
                rebuildActionEditor,
                promptOnRebuildFieldEditor,
                automaticallyRefreshEditor,
                spanEditor,
                loggingLevelFiedEditor,
                primeAWTInReports,
                showExclusionAnnotations,
                autoOpenCloverViews,
                onContextChangeActionEditor,
                promptOnContextChangeFieldEditor,
                onInstrumentationSourceChangeActionEditor,
                promptOnInstrumentationSourceChangeFieldEditor,
                preserveInstrSourcesEditor
            };
        }

        private void bindPreferencesStoreToEditors(IPreferenceStore store, FieldEditor[] editors) {
            for (FieldEditor editor : editors) {
                editor.setPreferenceStore(store);
            }
        }

        private void loadEditors(FieldEditor[] editors) {
            for (FieldEditor editor : editors) {
                editor.load();
            }
        }

        private void storeEditors(FieldEditor[] editors) {
            for (FieldEditor editor : editors) {
                editor.store();
            }
        }

        private void loadEditorDefaults(FieldEditor[] editors) {
            for (FieldEditor editor : editors) {
                editor.loadDefault();
            }
        }

        public void performDefaults() {
            loadEditorDefaults(allConfigurationEditors());
        }

        public void performApply() throws IOException {
            storeEditors(allConfigurationEditors());
            CloverPlugin.getInstance().getInstallationSettings().save();
        }

        public boolean performOk() throws IOException {
            performApply();
            return true;
        }
    }

    @Override
    protected void performApply() {
        try {
            panel.performApply();
        } catch (IOException e) {
            CloverPlugin.logError("Unable to persist preferences", e);
        }
    }


    @Override
    protected void performDefaults() {
        panel.performDefaults();
    }

    @Override
    public boolean performOk() {
        try {
            return panel.performOk();
        } catch (IOException e) {
            CloverPlugin.logError("Unable to persist preferences", e);
            return true;
        }
    }
}
