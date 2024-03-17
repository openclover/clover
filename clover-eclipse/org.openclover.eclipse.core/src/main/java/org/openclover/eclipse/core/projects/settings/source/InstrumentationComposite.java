package org.openclover.eclipse.core.projects.settings.source;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.openclover.core.cfg.instr.InstrumentationConfig;
import org.openclover.core.cfg.instr.InstrumentationLevel;
import org.openclover.core.cfg.instr.java.LambdaInstrumentation;
import org.openclover.core.util.ArrayUtil;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.eclipse.core.ui.CharDimensionConverter;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.core.ui.SwtUtils;

/**
 * Creates a panel with various instrumentation options, which can be set by user, like:
 *  - initstring value
 *  - output directory for classes
 *  - flush policy
 *  - references to java.lang classes
 */
public class InstrumentationComposite extends Composite {

    private Button projectOutputDirButton;
    private Button userOutputDirButton;
    private Text userOutputDirText;

    private Button defaultInitStringButton;
    private Button userInitStringButton;
    private Text initStringText;
    private Button userInitStringRelativeButton;

    private Button qualifyJavaLangButton;

    private Button flushPolicyDirectedButton;
    private Button flushPolicyIntervalButton;
    private Button flushPolicyThreadedButton;
    private Text intervalText;

    private Button recreateOutputDirButton;

    /** A Clover project associated with this widget. Can be null if Clover is not enabled for given project. */
    private CloverProject cloverProject;
    private ProjectSettings projectSettings;
    private CharDimensionConverter charDimensionConverter;
    private Button compileWithCloverButton;
    private Combo instrumentationLevelCombo;
    private Combo instrumentLambdaCombo;

    public InstrumentationComposite(Composite parent, CharDimensionConverter converter, 
                                    ProjectSettings properties, CloverProject cloverProject) throws CoreException {
        super(parent, SWT.NONE);
        this.cloverProject = cloverProject;
        this.charDimensionConverter = converter;
        this.projectSettings = properties;

        setLayout(new GridLayout(2, true));

        createInitstringGroup(properties, this);
        createFlushGroup(properties, this);
        createOutputDirGroup(properties, this);
        createMiscGroup(properties, this);
    }

    /**
     * Returns flush interval in miliseconds defined by user or default value in case of parse error.
     * Valid only if getFlushPolicy() returns INTERVAL_FLUSHING or THREADED_FLUSHING.
     * @return int - interval in miliseconds
     */
    public int getFlushInterval() throws NumberFormatException {
        return Integer.parseInt(intervalText.getText());
    }

    /**
     * Return flush policy selected.
     * @see InstrumentationConfig#DIRECTED_FLUSHING
     * @see InstrumentationConfig#INTERVAL_FLUSHING
     * @see InstrumentationConfig#THREADED_FLUSHING
     * @see InstrumentationConfig#DEFAULT_FLUSHING
     * @return int - flush policy
     */
    public int getFlushPolicy() {
        return flushPolicyDirectedButton.getSelection()
                ? InstrumentationConfig.DIRECTED_FLUSHING
                : flushPolicyIntervalButton.getSelection()
                ? InstrumentationConfig.INTERVAL_FLUSHING
                : flushPolicyThreadedButton.getSelection()
                ? InstrumentationConfig.THREADED_FLUSHING
                : InstrumentationConfig.DEFAULT_FLUSHING;
    }

    /**
     * Returns whether user default or custom initstring value is selected.
     * @return true if default, false if custom
     */
    public boolean isDefaultInitString() {
        return defaultInitStringButton.getSelection();
    }

    /**
     * Returns value of custom initstring. Has meaning only if isDefaultInitString() returns false.
     * @see #isDefaultInitString()
     * @return String, for example ".clover/coverage.db"
     */
    public String getCustomInitStringValue() {
        return (initStringText.getText() != null) ? initStringText.getText().trim() : "";
    }

    /**
     * Returns true if the "Relative to project dir" checkbox is selected, so that value returned by
     * getCustomInitStringValue() shall be treated as relative path. Has meaning only if isDefaultInitString()
     * returns false.
     * @see #isDefaultInitString()
     * @see #getCustomInitStringValue()
     * @return
     */
    public boolean isCustomInitStringRelative() {
        return userInitStringRelativeButton.getSelection();
    }

    /**
     * Returns whether instrumented classes shall be written to default project output directory
     * or to a custom location defined by user.
     * @return true project output dir, false custom output dir
     */
    public boolean isProjectOutputDir() {
        return projectOutputDirButton.getSelection();
    }

    /**
     * Returns a path relative to project root pointing to a directory where instrumented classes shall be stored.
     * Has meaning only if isProjectOutputDir() returns false.
     * @see #isProjectOutputDir()
     * @return
     */
    public String getCustomOutputDir() {
        return (userOutputDirText.getText() != null) ? userOutputDirText.getText().trim() : "";
    }

    /**
     * Returns true if original output folders shall be recreated in custom output location.
     * Has meaning only if isProjectOutputDir() returns false.
     * @see #isProjectOutputDir()
     * @see #getCustomOutputDir()
     * @return true if recreate
     */
    public boolean isCustomOutputDirRecreateOriginal() {
        return recreateOutputDirButton.getSelection();
    }

    /**
     * Returns whether any references to java.lang.* classes shall be fully qualified in instrumented code.
     * @return true if shall be fully qualified, false otherwise
     */
    public boolean isQualifyJavaLangReferences() {
        return qualifyJavaLangButton.getSelection();
    }

    public boolean isInstrumentationEnabled() {
        return compileWithCloverButton.getSelection();
    }

    public InstrumentationLevel getInstrumentationLevel() {
        return instrumentationLevelCombo.getSelectionIndex() == 0
                ? InstrumentationLevel.STATEMENT
                : InstrumentationLevel.METHOD;
    }

    public LambdaInstrumentation getInstrumentLambda() {
        int index = instrumentLambdaCombo.getSelectionIndex();
        if (index >= 0 && index < LambdaInstrumentation.values().length) {
            return LambdaInstrumentation.values()[instrumentLambdaCombo.getSelectionIndex()];
        } else {
            // out of bounds (hmm ... a plugin upgrade with new enum values)? just use default
            return ProjectSettings.Defaults.INSTRUMENT_LAMBDA;
        }
    }

    public boolean isRebuildRequired() {
        return isMiscSettingsRebuildRequired() || isInstrumentationSettingsRebuildRequired() || isFlushSettingsRebuildRequired();
    }

    private boolean isFlushSettingsRebuildRequired() {
        return (getFlushPolicy() != projectSettings.getFlushPolicy())
                || (intervalText.isEnabled() && !intervalText.getText().equals(Integer.toString(projectSettings.getFlushInterval())));
    }

    private boolean isInstrumentationSettingsRebuildRequired() {
        return (defaultInitStringButton.getSelection() != projectSettings.isInitStringDefault())
                || (userInitStringButton.getSelection() == projectSettings.isInitStringDefault())
                || (!initStringText.getText().equals(projectSettings.getInitString()))
                || (userInitStringRelativeButton.getSelection() != projectSettings.isInitStringProjectRelative());
    }

    private boolean isMiscSettingsRebuildRequired() {
        return projectSettings.isInstrumentationEnabled() != isInstrumentationEnabled()
                || projectSettings.getInstrumentationLevel() != getInstrumentationLevel()
                || projectSettings.getInstrumentLambda() != getInstrumentLambda();
    }


    private void createInitstringGroup(final ProjectSettings properties, final Composite panel) throws CoreException {
        final Group initStringGroup = LayoutUtils.createGroup(panel, CloverEclipsePluginMessages.INITSTRING());
        initStringGroup.setLayout(new GLH().standardiseMargin().getGridLayout());
        initStringGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        SwtUtils.createMultilineLabel(
                initStringGroup,
                CloverEclipsePluginMessages.INITSTRING_BLURB(),
                charDimensionConverter.convertWidthInCharsToPixels(45));

        final boolean defaultInitString = properties.isInitStringDefault();
        final String initString = properties.getInitString();

        defaultInitStringButton = new Button(initStringGroup, SWT.RADIO);
        defaultInitStringButton.setText(CloverEclipsePluginMessages.INITSTRING_AUTOMATIC());
        defaultInitStringButton.setSelection(defaultInitString);
        defaultInitStringButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                initStringText.setEnabled(!defaultInitStringButton.getSelection());
                userInitStringRelativeButton.setEnabled(!defaultInitStringButton.getSelection());
            }
        });

        userInitStringButton = new Button(initStringGroup, SWT.RADIO);
        userInitStringButton.setText(CloverEclipsePluginMessages.INITSTRING_USER_SPECIFIED());
        userInitStringButton.setSelection(!defaultInitString);

        initStringText = new Text(initStringGroup, SWT.BORDER);
        initStringText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        initStringText.setEnabled(!defaultInitString);
        initStringText.setText(initString == null ? "" : initString);

        userInitStringRelativeButton = new Button(initStringGroup, SWT.CHECK);
        userInitStringRelativeButton.setText(CloverEclipsePluginMessages.INITSTRING_RELATIVE());
        userInitStringRelativeButton.setSelection(properties.isInitStringProjectRelative());
        userInitStringRelativeButton.setEnabled(!defaultInitString);
    }

    private void createOutputDirGroup(final ProjectSettings properties, final Composite panel) throws CoreException {
        final Group outputDirGroup = LayoutUtils.createGroup(panel, CloverEclipsePluginMessages.OUTPUT_FOLDER_GROUP());
        outputDirGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        outputDirGroup.setLayout(new GLH(2, false).standardiseMargin().getGridLayout());

        final Label label = new Label(outputDirGroup, SWT.NONE);
        label.setText(CloverEclipsePluginMessages.OUTPUT_DIR());

        final boolean outputToProjectDir = properties.isOutputRootSameAsProject();
        String outputRoot = properties.getOutputRoot();

        projectOutputDirButton = new Button(outputDirGroup, SWT.RADIO);
        SwtUtils.setHorizontalSpan(projectOutputDirButton, 2);
        projectOutputDirButton.setText(CloverEclipsePluginMessages.PLACE_IN_PROJECT_DIRS());
        projectOutputDirButton.setSelection(outputToProjectDir);

        userOutputDirButton = new Button(outputDirGroup, SWT.RADIO);
        userOutputDirButton .setLayoutData(new GridData());
        SwtUtils.setHorizontalSpan(userOutputDirButton, 2);
        userOutputDirButton.setText(CloverEclipsePluginMessages.PLACE_IN_USER_DIRS());
        userOutputDirButton.setSelection(!outputToProjectDir);

        userOutputDirText = new Text(outputDirGroup, SWT.BORDER);
        userOutputDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        userOutputDirText.setEnabled(userOutputDirButton.getSelection());
        userOutputDirText.setText(outputRoot == null ? "" : outputRoot);

        final Button dirButton = new Button(outputDirGroup, SWT.NONE);
        dirButton.setLayoutData(new GridData());
        dirButton.setText("...");
        dirButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                final ContainerSelectionDialog dialog =
                        new ContainerSelectionDialog(
                                getShell(),
                                /*getCloverProject().getProject()*/null,
                                true,
                                CloverEclipsePluginMessages.OUTPUT_FOLDER_DIALOG_CAPTION());

                dialog.showClosedProjects(false);
                dialog.setValidator(o -> {
                    final IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember((IPath)o);
                    final IProject project = container.getProject();
                    if ( (project != container) && (project == cloverProject.getProject())) {
                        return null;
                    } else {
                        return CloverEclipsePluginMessages.ERROR_SELECT_WITHIN_PROJECT();
                    }
                });
                if (dialog.open() == IDialogConstants.OK_ID) {
                    IPath selection = (IPath)dialog.getResult()[0];
                    userOutputDirText.setText(
                            selection.removeFirstSegments(cloverProject.getProject().getFullPath().segmentCount()).toString());
                }
            }
        });

        userOutputDirButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                userOutputDirText.setEnabled(userOutputDirButton.getSelection());
                recreateOutputDirButton.setEnabled(userOutputDirButton.getSelection());
                dirButton.setEnabled(userOutputDirButton.getSelection());
            }
        });

        recreateOutputDirButton = new Button(outputDirGroup, SWT.CHECK);
        recreateOutputDirButton.setText(CloverEclipsePluginMessages.OUTPUT_FOLDER_RECREATE_ORIGINAL_FOLDERS());
        recreateOutputDirButton.setToolTipText(CloverEclipsePluginMessages.OUTPUT_FOLDER_RECREATE_ORIGINAL_FOLDERS_TOOLTIP());
        recreateOutputDirButton.setEnabled(!outputToProjectDir);
        recreateOutputDirButton.setSelection(properties.isRecreateOutputDirs());
    }

    private void createMiscGroup(final ProjectSettings properties, final Composite panel) {
        final Group miscGroup = LayoutUtils.createGroup(panel, CloverEclipsePluginMessages.MISC_INSTRUMENTATION());
        miscGroup.setLayout(new GLH(1, false).standardiseMargin().getGridLayout());
        miscGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        createQualifyJavaLangWidget(properties, miscGroup);
        createInstrumentationLevelWidget(properties, miscGroup);
        createIntrumentLambdaWidget(properties, miscGroup);
    }

    /**
     * Just create a label.
     */
    private void createQualifyJavaLangWidget(final ProjectSettings properties, final Composite parent) {
        qualifyJavaLangButton = new Button(parent, SWT.CHECK);
        qualifyJavaLangButton.setText(CloverEclipsePluginMessages.QUALIFY_JAVA_LANG());
        qualifyJavaLangButton.setSelection(properties.shouldQualifyJavaLang());
    }

    /**
     * Create "Instrument at compile at: statement level / method level" widget
     */
    private void createInstrumentationLevelWidget(final ProjectSettings properties, final Composite parent) {
        final Composite instrumentationComposite = new Composite(parent, SWT.NONE);
        instrumentationComposite.setLayout(new GLH(2, false).marginWidth(0).getGridLayout());

        compileWithCloverButton = new Button(instrumentationComposite, SWT.CHECK);
        compileWithCloverButton.setText("Instrument and compile at ");
        compileWithCloverButton.setToolTipText(
                "Choose whether OpenClover should instrument and compile your source code and what coverage granularity.\n\n" +
                "This can be switched off if you wish to develop without tracking code coverage for a period of time or " +
                "if you have configured OpenClover Ant or Maven integration to instrument and compile your source for you."
        );
        compileWithCloverButton.setSelection(properties.isInstrumentationEnabled());

        instrumentationLevelCombo = new Combo(instrumentationComposite, SWT.CHECK | SWT.READ_ONLY);
        instrumentationLevelCombo.setItems(new String[]{"statement level", "method level"});
        instrumentationLevelCombo.select(properties.getInstrumentationLevel() == InstrumentationLevel.STATEMENT ? 0 : 1);
        instrumentationLevelCombo.setToolTipText(
                "Statement level instrumentation is more accurate but has a runtime performance penalty." +
                "Method level instrumentation is less accurate but will run faster and OpenClover will be able to provide coverage feedback more swiftly.\n\n" +
                "If you only use OpenClover for optimizing your test runs, method-level instrumentation is the best option.");
        instrumentationLevelCombo.setEnabled(properties.isInstrumentationEnabled());

        final SelectionListener onChangeEnableLevelCombo = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                instrumentationLevelCombo.setEnabled(compileWithCloverButton.getSelection());
            }
        };
        compileWithCloverButton.addSelectionListener(onChangeEnableLevelCombo);
    }

    /**
     * Create "Instrument lambda" label + drop-down with values
     */
    private void createIntrumentLambdaWidget(final ProjectSettings properties, final Composite parent) {
        final Composite box = new Composite(parent, SWT.NONE);
        box.setLayout(new GLH(2, false).marginWidth(0).getGridLayout());

        final Label instrumentLambdaLabel = new Label(box, 0);
        instrumentLambdaLabel.setText("Instrument lambda functions ");

        instrumentLambdaCombo = new Combo(box, SWT.CHECK | SWT.READ_ONLY);
        instrumentLambdaCombo.setItems(ArrayUtil.toLowerCaseStringArray(LambdaInstrumentation.values()));
        instrumentLambdaCombo.select(properties.getInstrumentLambda().ordinal());
        instrumentLambdaCombo.setToolTipText(
                "Select whether lambda functions introduced in Java 8 shall be instrumented by OpenClover so that you can track \n" +
                "code coverage for them and show them in reports similarly as normal methods. \n" +
                "You can also limit instrumentation to certain form of lambda functions: \n" +
                " * written as expressions, e.g. '(a + b) -> a + b' \n" +
                " * written as code blocks, e.g. '() -> { return xyz(); }' \n" +
                " * written in any form except method references, e.g. 'Math::abs'");
    }

    private void createFlushGroup(final ProjectSettings properties, final Composite panel) throws CoreException {
        final Group flushPolicyGroup = LayoutUtils.createGroup(panel, CloverEclipsePluginMessages.FLUSH_POLICY());
        flushPolicyGroup.setLayout(new GLH(2, false).standardiseMargin().getGridLayout());
        flushPolicyGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Label explanationLabel =
                SwtUtils.createMultilineLabel(
                        flushPolicyGroup,
                        CloverEclipsePluginMessages.FLUSH_POLICY_EXPLANATION(),
                        charDimensionConverter.convertWidthInCharsToPixels(45));
        SwtUtils.setHorizontalSpan(explanationLabel, 2);

        final int flushPolicy = properties.getFlushPolicy();

        flushPolicyDirectedButton = new Button(flushPolicyGroup, SWT.RADIO);
        flushPolicyDirectedButton.setText(CloverEclipsePluginMessages.FLUSH_POLICY_DIRECTED());
        flushPolicyDirectedButton.setSelection(flushPolicy == InstrumentationConfig.DIRECTED_FLUSHING);
        SwtUtils.setHorizontalSpan(flushPolicyDirectedButton, 2);

        flushPolicyIntervalButton = new Button(flushPolicyGroup, SWT.RADIO);
        flushPolicyIntervalButton.setText(CloverEclipsePluginMessages.FLUSH_POLICY_INTERVAL());
        flushPolicyIntervalButton.setSelection(flushPolicy == InstrumentationConfig.INTERVAL_FLUSHING);
        flushPolicyIntervalButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                intervalText.setEnabled(
                        flushPolicyIntervalButton.getSelection()
                                || flushPolicyThreadedButton.getSelection());
            }
        });
        SwtUtils.setHorizontalSpan(flushPolicyIntervalButton, 1);

        final Composite intervalComposite = new Composite(flushPolicyGroup, SWT.NONE);
        intervalComposite.setLayout(new GridLayout(2, true));
        GridData gridData = SwtUtils.gridDataFor(intervalComposite);
        gridData.horizontalSpan = 1;
        gridData.verticalSpan = 2;
        gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;

        intervalText = new Text(intervalComposite, SWT.BORDER);
        intervalText.setText(Integer.toString(properties.getFlushInterval()));
        gridData = SwtUtils.gridDataFor(intervalText);
        gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = 50;

        final Label msLabel = new Label(intervalComposite, SWT.NONE);
        msLabel.setText(CloverEclipsePluginMessages.FLUSH_POLICY_INTERVAL_MILISECONDS());

        flushPolicyThreadedButton = new Button(flushPolicyGroup, SWT.RADIO);
        flushPolicyThreadedButton.setText(CloverEclipsePluginMessages.FLUSH_POLICY_THREADED());
        flushPolicyThreadedButton.setSelection(flushPolicy == InstrumentationConfig.THREADED_FLUSHING);
        flushPolicyThreadedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                intervalText.setEnabled(
                        flushPolicyIntervalButton.getSelection()
                                || flushPolicyThreadedButton.getSelection());
            }
        });
        SwtUtils.setHorizontalSpan(flushPolicyThreadedButton, 1);

        intervalText.setEnabled(
                flushPolicyIntervalButton.getSelection()
                        || flushPolicyThreadedButton.getSelection());
    }

}
