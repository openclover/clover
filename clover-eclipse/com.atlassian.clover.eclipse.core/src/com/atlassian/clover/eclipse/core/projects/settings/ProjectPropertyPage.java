package com.atlassian.clover.eclipse.core.projects.settings;

import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.projects.settings.source.InstrumentSourceFilteringComposite;
import com.atlassian.clover.eclipse.core.projects.settings.source.InstrumentationComposite;
import com.atlassian.clover.eclipse.core.projects.settings.source.SourceFolderPattern;
import com.atlassian.clover.eclipse.core.projects.settings.source.SourceRootsWithPatternTreeContentProvider;
import com.atlassian.clover.eclipse.core.projects.settings.source.test.TestSourceFilteringComposite;
import com.atlassian.clover.eclipse.core.ui.GLH;
import com.atlassian.clover.eclipse.core.ui.widgets.ContextFilterModificationWidget;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import java.util.List;

public class ProjectPropertyPage extends BaseSettingsPage implements IWorkbenchPropertyPage {
    public static final String ID = CloverPlugin.ID + ".properties.cloverProjectPropertyPage";
    private static final QualifiedName LAST_SELECTED_TAB_KEY = new QualifiedName(ID, "lastSelectedTab");
    private static final int SOURCE_TAB_INDEX = 2;

    private Panel panel;

    @Override
    protected Control createContents(Composite composite) {
        try {
            return panel = new Panel(composite, this);
        } catch (CoreException e) {
            CloverPlugin.logError("Error creating project properties panel", e);
            return null;
        }
    }

    public static void focusSourceTab(IProject project) throws CoreException {
        project.getProject().setSessionProperty(LAST_SELECTED_TAB_KEY, SOURCE_TAB_INDEX);
    }
    
    public class Panel extends Composite {
        private IJavaProject project;

        private Button enableButton;

        private InstrumentationComposite instrumentationComposite;
        private InstrumentSourceFilteringComposite instrumentSourceFilteringComposite;
        private TestSourceFilteringComposite testSourceFilteringComposite;
        private ContextFilterModificationWidget contextFilter;
        public static final int HORIZONTAL_INDENT = 30;

        public Panel(Composite composite, PropertyPage propertyPage) throws CoreException {
            super(composite, SWT.NONE);

            project = getJavaProject();

            setLayout(new GLH().marginHeight(0).marginWidth(0).getGridLayout());

            GridData gridData = new GridData();
            gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
            gridData.grabExcessHorizontalSpace = true;

            enableButton = new Button(this, SWT.CHECK);
            enableButton.setText(CloverEclipsePluginMessages.ENABLE_CLOVER_COVERAGE());
            enableButton.setLayoutData(gridData);

            gridData = new GridData();
            gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
            gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
            gridData.grabExcessHorizontalSpace = true;
            gridData.grabExcessVerticalSpace = true;

            final TabFolder tabFolder = new TabFolder(this, SWT.NONE);
            tabFolder.setLayoutData(gridData);

            ProjectSettings settings = new ProjectSettings(project.getProject());

            instrumentationComposite = createInstrumentationTab(settings, tabFolder);
            contextFilter = createFilteringTab(settings, tabFolder);

            final List<SourceFolderPattern> patterns = settings.getInstrumentedFolderPatterns();
            SourceRootsWithPatternTreeContentProvider instrumentedSrcRootsContentProvider =
                    new SourceRootsWithPatternTreeContentProvider(project, patterns);
            instrumentSourceFilteringComposite = createSourceTab(settings, tabFolder, instrumentedSrcRootsContentProvider);
            testSourceFilteringComposite = createTestsTab(settings, tabFolder, instrumentedSrcRootsContentProvider);

            tabFolder.pack();
            final Object lastIdx = project.getProject().getSessionProperty(LAST_SELECTED_TAB_KEY);
            if (lastIdx instanceof Integer) {
                tabFolder.setSelection((Integer) lastIdx);
            }
            tabFolder.addListener(SWT.Selection, new Listener() {

                @Override
                public void handleEvent(Event event) {
                    try {
                        project.getProject().setSessionProperty(LAST_SELECTED_TAB_KEY, new Integer(tabFolder.getSelectionIndex()));
                    } catch (CoreException e1) {
                        // ignore
                    }
                }
                
            });
            
            enableButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent selectionEvent) {
                    tabFolder.setEnabled(enableButton.getSelection());
                }
            });

            try {
                enableButton.setSelection(CloverProject.isAppliedTo(project));
                tabFolder.setEnabled(enableButton.getSelection());
            } catch (CoreException e) {
                CloverPlugin.logError("Error initialising project properties panel", e);
                throw e;
            }
        }

        private IJavaProject getJavaProject() throws CoreException {
            return (IJavaProject)((IProject)getElement()).getNature(JavaCore.NATURE_ID);
        }

        /**
         * Returns a Clover project associated with given element.
         * @return
         * @throws CoreException
         */
        private CloverProject getCloverProject() throws CoreException {
            return CloverProject.getFor((IProject)getElement());
        }

        /**
         * Creates "Instrumentation" tab.
         * @param properties
         * @param tabFolder
         * @return
         * @throws CoreException
         */
        private InstrumentationComposite createInstrumentationTab(ProjectSettings properties, TabFolder tabFolder) throws CoreException {
            final TabItem compilationItem = new TabItem(tabFolder, SWT.NULL);
            compilationItem.setText(CloverEclipsePluginMessages.INSTRUMENTATION());

            final InstrumentationComposite composite =
                    new InstrumentationComposite(tabFolder, ProjectPropertyPage.this, properties, getCloverProject());
            compilationItem.setControl(composite);
            return composite;
        }

        /**
         * Creates "Source Files" tab.
         * @param properties
         * @param tabFolder
         * @param contentProvider
         * @return
         */
        private InstrumentSourceFilteringComposite createSourceTab(ProjectSettings properties, TabFolder tabFolder, SourceRootsWithPatternTreeContentProvider contentProvider) {
            final TabItem sourceItem = new TabItem(tabFolder, SWT.NULL, SOURCE_TAB_INDEX);
            sourceItem.setText(CloverEclipsePluginMessages.SOURCE_FILES_TAB());

            final InstrumentSourceFilteringComposite composite =
                    new InstrumentSourceFilteringComposite(tabFolder, properties, contentProvider);
            composite.setLayout(new GridLayout(1, true));
            sourceItem.setControl(composite);

            return composite;
        }

        /**
         * Creates "Test Classes" tab.
         * @param properties
         * @param tabFolder
         * @param contentProvider
         * @return TestSourceFilteringComposite a composite object which is placed inside new tab
         */
        private TestSourceFilteringComposite createTestsTab(ProjectSettings properties, TabFolder tabFolder, SourceRootsWithPatternTreeContentProvider contentProvider) {
            final TabItem testsItem = new TabItem(tabFolder, SWT.NULL);
            testsItem.setText(CloverEclipsePluginMessages.TEST_CLASSES_TAB());

            final TestSourceFilteringComposite composite =
                    new TestSourceFilteringComposite(tabFolder, project, properties, contentProvider);
            composite.setLayout(new GridLayout(1, true));
            testsItem.setControl(composite);

            return composite;
        }


        /**
         * Creates "Contexts" tab.
         * @param properties
         * @param tabFolder
         * @return
         * @throws CoreException
         */
        private ContextFilterModificationWidget createFilteringTab(ProjectSettings properties, TabFolder tabFolder) throws CoreException {
            TabItem filteringItem = new TabItem(tabFolder, SWT.NULL);
            filteringItem.setText(CloverEclipsePluginMessages.FILTERING());

            Composite filteringTabPanel = new Composite(tabFolder, SWT.NONE);
            filteringTabPanel.setLayout(new GridLayout(1, true));
            filteringItem.setControl(filteringTabPanel);

            final ContextFilterModificationWidget contextFilter = new ContextFilterModificationWidget(filteringTabPanel, properties);
            contextFilter.setLayoutData(new GridData(GridData.FILL_BOTH));
            return contextFilter;
        }



        public boolean performCancel() {
            return true;
        }

        public boolean performApply() {
            if (okToLeave()) {
                try {
                    ProjectSettings properties = new ProjectSettings(project.getProject());

                    boolean rebuild = contextFilter.alertIfManualRebuildRequired();
                    contextFilter.store();

                    if (!rebuild && (instrumentSourceFilteringComposite.isModified() || testSourceFilteringComposite.isModified())) {
                        rebuild = InstrumentSourceFilteringComposite.askForRebuild();
                    }

                    instrumentSourceFilteringComposite.storeTo(properties);
                    testSourceFilteringComposite.storeTo(properties);

                    if (!rebuild && instrumentationComposite.isRebuildRequired()) {
                        rebuild = getCloverProject().okayToRebuild(getShell());
                    }
                    properties.shouldQualifyJavaLang(instrumentationComposite.isQualifyJavaLangReferences());
                    properties.setInstrumentationEnabled(instrumentationComposite.isInstrumentationEnabled());
                    properties.setInstrumentationLevel(instrumentationComposite.getInstrumentationLevel());
                    properties.setInstrumentLambda(instrumentationComposite.getInstrumentLambda());

                    properties.updateInitString(instrumentationComposite.isDefaultInitString(),
                            instrumentationComposite.getCustomInitStringValue(),
                            instrumentationComposite.isCustomInitStringRelative());

                    int flushInterval;
                    try {
                        flushInterval = instrumentationComposite.getFlushInterval();
                    } catch (NumberFormatException e) {
                        flushInterval = ProjectSettings.Defaults.FLUSH_INTERVAL;
                    }
                    properties.updateFlushPolicy(instrumentationComposite.getFlushPolicy(),
                            flushInterval);

                    properties.updateOutputRoot(instrumentationComposite.isProjectOutputDir(),
                            instrumentationComposite.getCustomOutputDir(),
                            instrumentationComposite.isCustomOutputDirRecreateOriginal());



                    //If button and nature are inconsistent, toggle nature
                    if (enableButton.getSelection() ^ CloverProject.isAppliedTo(project)) {
                        return CloverProject.toggleWithUserFeedback(getShell(), project);
                    }

                    if (rebuild) {
                        try {
                            properties.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
                        } catch (CoreException e) {
                            CloverPlugin.logError("Unable to rebuild project", e);
                        }
                    }

                } catch (CoreException e) {
                    CloverPlugin.logError("Error applying properties", e);
                    setMessage(CloverEclipsePluginMessages.ERROR_UNEXPECTED_WHILE_APPLY_PROPERTIES());
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }

        public boolean performOk() {
            return performApply();
        }

        public void performDefaults() {

        }

        public boolean okToLeave() {
            boolean okToLeave = false;

            errorWithValue: {
                if ( (instrumentationComposite.isDefaultInitString() == false)
                        && (instrumentationComposite.getCustomInitStringValue().length() == 0) ) {
                    setMessage(CloverEclipsePluginMessages.ERROR_CUSTOM_INITSTRING_NOT_EMPTY(), IMessageProvider.ERROR);
                    break errorWithValue;
                }

                if ( (instrumentationComposite.getFlushPolicy() == InstrumentationConfig.INTERVAL_FLUSHING)
                        || (instrumentationComposite.getFlushPolicy() == InstrumentationConfig.THREADED_FLUSHING) ) {
                    try {
                        int flushInterval = instrumentationComposite.getFlushInterval();
                        if (flushInterval < 0) {
                            setMessage(CloverEclipsePluginMessages.ERROR_FLUSH_INTERVAL_NOT_POSITIVE(), IMessageProvider.ERROR);
                            break errorWithValue;
                        }
                    } catch (NumberFormatException e) {
                        setMessage(CloverEclipsePluginMessages.ERROR_FLUSH_INTERVAL_NOT_A_NUMBER(), IMessageProvider.ERROR);
                        break errorWithValue;
                    }
                }

                if ( (instrumentationComposite.isProjectOutputDir() == false)
                        && (instrumentationComposite.getCustomOutputDir().length() == 0) ) {
                    setMessage(CloverEclipsePluginMessages.ERROR_OUTPUT_DIR_NAME_IS_NULL(), IMessageProvider.ERROR);
                    break errorWithValue;
                }

                // CLOV-1083: Make sure that user will not set output directory for instrumented sources the same
                // as original source dir. Otherwise it would delete original files during project cleanup.
                if ( (instrumentationComposite.isProjectOutputDir() == false)
                        && (instrumentationComposite.getCustomOutputDir().length() > 0) ) {

                    IPath path;
                    IPackageFragment packageFragment;
                    try {
                        path = Path.fromOSString(instrumentationComposite.getCustomOutputDir());
                        packageFragment = project.findPackageFragment(project.getPath().append(path));
                    } catch (JavaModelException ex) {
                        CloverPlugin.logError("Error applying properties", ex);
                        setMessage(CloverEclipsePluginMessages.ERROR_UNEXPECTED_WHILE_CUSTOM_OUTPUT_DIR());
                        return false;
                    }
                    if (packageFragment != null) {
                        CloverPlugin.logDebug(CloverEclipsePluginMessages.ERROR_OUTPUT_FOLDER_HAS_SOURCES(path.toString(), packageFragment.getElementName()));
                        setMessage(CloverEclipsePluginMessages.ERROR_OUTPUT_FOLDER_HAS_SOURCES(), IMessageProvider.ERROR);
                        break errorWithValue;
                    }
                }

                okToLeave = true;
                setMessage(null);
            }

            return okToLeave;
        }
    }

    @Override
    protected void performApply() {
        if (panel != null) {
            panel.performApply();
        }
    }

    @Override
    public boolean performOk() {
        return panel == null || panel.performOk();
    }

    @Override
    protected void performDefaults() {
        if (panel != null) {
            panel.performDefaults();
        }
    }

    @Override
    public boolean performCancel() {
        return panel == null || panel.performCancel();
    }


    @Override
    public boolean okToLeave() {
        return panel == null || panel.okToLeave();
    }
}
