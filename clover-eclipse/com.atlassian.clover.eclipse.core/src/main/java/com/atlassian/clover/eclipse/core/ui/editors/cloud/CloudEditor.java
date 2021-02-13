package com.atlassian.clover.eclipse.core.ui.editors.cloud;

import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.ui.CloverPluginIcons;
import com.atlassian.clover.eclipse.core.ui.editors.CloverProjectInput;
import com.atlassian.clover.eclipse.core.ui.projects.DatabaseChangeEvent;
import com.atlassian.clover.eclipse.core.ui.projects.DatabaseChangeListener;
import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;
import com.atlassian.clover.eclipse.core.views.actions.OpenJavaEditorAction;
import com.atlassian.clover.registry.metrics.BlockMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsSupport;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.File;

public class CloudEditor
    extends MultiPageEditorPart
    implements IResourceChangeListener {

    public static final String ID = CloverPlugin.ID + ".editors.cloud";
    private static final int QUICK_WINS_PAGE = 1;
    private static final int PROJECT_RISKS_PAGE = 0;

    private Map<String, PackageCloudStructure> perPackageCloudMappings;
    private Browser[] browsers = new Browser[2];
    private LocationListener locationListener = EditorLinkingLocationListener.NO_LINKING;
    private OpenJavaEditorAction openAction;
    private Combo focusDropDown;
    private Button aggregateCheckbox;
    private Button refreshButton;

    private DatabaseChangeListener coverageListener = new DatabaseChangeListener() {
    @Override
    public void databaseChanged(DatabaseChangeEvent event) {
        if (event.isApplicableTo(((CloverProjectInput) getEditorInput()).getProject())
            && event.isSubstantiveProjectChange()) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    CloudEditor.this.onCoverageChanged();
                }
            });
        }
    }
    };

    public CloudEditor() {
        super();
    }

    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        super.init(site, editorInput);

        if (!(editorInput instanceof CloudProjectInput)) {
            throw new PartInitException("Invalid Input: must be CloudInput");
        }

        openAction = new OpenJavaEditorAction(getSite().getPage().getActivePart().getSite());

        locationListener = new EditorLinkingLocationListener(
            ((CloverProjectInput) editorInput).getProject(), openAction);
    }

    private void createProjectRisksPage() {
        browsers[PROJECT_RISKS_PAGE] = new Browser(getContainer(), SWT.NONE);
        browsers[PROJECT_RISKS_PAGE].setUrl(getProjectRisksURI());
        browsers[PROJECT_RISKS_PAGE].addLocationListener(locationListener);
        setPageText(addPage(browsers[PROJECT_RISKS_PAGE]), CloverEclipsePluginMessages.PROJECT_RISKS());
    }

    private void createQuickWinsPage() {
        browsers[QUICK_WINS_PAGE] = new Browser(getContainer(), SWT.NONE);
        browsers[QUICK_WINS_PAGE].setUrl(getProjectQuickWinsURI());
        browsers[QUICK_WINS_PAGE].addLocationListener(locationListener);
        setPageText(addPage(browsers[QUICK_WINS_PAGE]), CloverEclipsePluginMessages.QUICK_WINS());
    }

    @Override
    protected void createPages() {
        updatePerPackageCloudMapping();

        createProjectRisksPage();
        createQuickWinsPage();

        //Sneaky but effective: take this (one and only) opportunity
        //to replace the FillLayout with a GridLayout so our decorations
        //don't make the whole thing look shite.
        getContainer().getParent().setLayout(new GridLayout(1, false));
        getContainer().setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite bottomBar = new Composite(getContainer().getParent(), SWT.NONE);
        bottomBar.setLayout(new GridLayout());

        Composite selectionBar = new Composite(bottomBar, SWT.NONE);
        selectionBar.setLayout(new GridLayout(2, false));

        //While we're here let's add the dropdown, checkbox and refresh button...
        createFocusDropDown(selectionBar);
        createAggregateCheckbox(selectionBar);
        createRefreshButton(bottomBar);

        populateFocusDropDown();

        CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(coverageListener);
    }

    private void createAggregateCheckbox(Composite selectionBar) {
        aggregateCheckbox = new Button(selectionBar, SWT.CHECK);
        aggregateCheckbox.setText(CloverEclipsePluginMessages.SHOW_AGGREGATE_CLOUD());
        aggregateCheckbox.setSelection(true);
        aggregateCheckbox.setEnabled(false);
        aggregateCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                refresh(false);
            }
        });
    }

    private void createFocusDropDown(Composite selectionBar) {
        focusDropDown = new Combo(selectionBar, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        focusDropDown.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        focusDropDown.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                refresh(false);
            }
        });
    }

    private void updatePerPackageCloudMapping() {
        final Map<String, PackageCloudStructure> perPackageCloudMappings = new LinkedHashMap<String, PackageCloudStructure>();
        final FullProjectInfo project = ((CloudProjectInput) getEditorInput()).getProject().getModel().getAppOnlyProjectInfo();
        final File outputDir = getReportBaseFolder();

        addCloudMap(perPackageCloudMappings, getProjectCloudName(), outputDir, false);

        final List<? extends PackageInfo> packages = project.getAllPackages();
        Collections.sort(packages, HasMetricsSupport.CMP_LEX);

        for (final PackageInfo pkg : packages) {
            addCloudMap(perPackageCloudMappings, pkg.getName(), new File(outputDir, pkg.getPath()),
                    pkg.getClasses().size() < pkg.getClassesIncludingSubPackages().size());
        }
        this.perPackageCloudMappings = perPackageCloudMappings;
    }

    private void addCloudMap(Map<String, PackageCloudStructure> pageMaps, String name, File outputDir, boolean hasDescendantClasses) {
        pageMaps.put(name, new PackageCloudStructure(name, outputDir, hasDescendantClasses));
    }

    private void populateFocusDropDown() {
        String oldSelection = focusDropDown.getText();

        focusDropDown.clearSelection();
        focusDropDown.removeAll();

        for (PackageCloudStructure cloud : perPackageCloudMappings.values()) {
            focusDropDown.add(cloud.name);
        }

        if (oldSelection != null && oldSelection.length() > 0) {
            focusDropDown.setText(oldSelection);
        } else {
            focusDropDown.select(0);
        }
    }

    private void createRefreshButton(Composite bottomBar) {
        refreshButton = new Button(bottomBar, SWT.NONE);
        refreshButton.setImage(CloverPlugin.getImage(CloverPluginIcons.PROJECT_REFRESH_ICON));
        refreshButton.setText(CloverEclipsePluginMessages.REFRESH_CLOUD_BUTTON());
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                new RefreshCloudEditorGenerateCloudJob(
                    CloudEditor.this,
                    ((CloverProjectInput) getEditorInput()).getProject().getProject()).schedule();
            }
        });
    }

    private String getProjectCloudName() {
        return "Project " + ((CloudProjectInput) getEditorInput()).getProject().getModel().getAppOnlyProjectInfo().getName();
    }

    private File getReportBaseFolder() {
        return getReportDir().getRawLocation().toFile();
    }

    private String getProjectRisksURI() {
        return EclipseCloudGenerator.getRisksURIFor(getReportBaseFolder(), true);
    }

    public String getProjectQuickWinsURI() {
        return EclipseCloudGenerator.getQuickWinsURIFor(getReportBaseFolder(), true);
    }

    private void onCoverageChanged() {
        BlockMetrics metrics =
            MetricsScope.FULL.getMetricsFor(
                ((CloverProjectInput)getEditorInput()).getProject().getProject());
        
        boolean coverageAvailable = metrics != null;
        refreshButton.setEnabled(coverageAvailable);
    }


    @Override
    public void dispose() {
        super.dispose();
        CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(coverageListener);
    }


    @Override
    public void doSave(IProgressMonitor monitor) {
        //No-op
    }

    @Override
    public void doSaveAs() {
        //No-op - we don't save
    }

    private IFolder getReportDir() {
        return ((CloverProjectInput) getEditorInput()).getProject().getCloverWorkingDir().getFolder("report");
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
    }

    public void refresh(final boolean regenerate) {
        Display.getDefault().asyncExec(
            new Runnable() {
                @Override
                public void run() {
                    if (regenerate) {
                        updatePerPackageCloudMapping();
                        populateFocusDropDown();
                    }

                    String selectedCloud = focusDropDown.getText();
                    PackageCloudStructure page = perPackageCloudMappings.get(selectedCloud);
                    if (page != null) {
                        aggregateCheckbox.setEnabled(page.hasSubpackages);

                        boolean showAggregateCloud = !page.hasSubpackages || aggregateCheckbox.getSelection();

                        browsers[PROJECT_RISKS_PAGE].setUrl(EclipseCloudGenerator.getRisksURIFor(page.outputDir, showAggregateCloud));
                        browsers[QUICK_WINS_PAGE].setUrl(EclipseCloudGenerator.getQuickWinsURIFor(page.outputDir, showAggregateCloud));

                        setPageText(
                            PROJECT_RISKS_PAGE,
                            page.name.equals(getProjectCloudName())
                                ? CloverEclipsePluginMessages.PROJECT_RISKS()
                                : CloverEclipsePluginMessages.PACKAGE_RISKS());
                    }
                }
            });
    }

    private static final class PackageCloudStructure {
        public final String name;
        public final File outputDir;
        public final boolean hasSubpackages;
        PackageCloudStructure(String name, File outputDir, boolean hasSubpackages) {
            this.name = name;
            this.outputDir = outputDir;
            this.hasSubpackages = hasSubpackages;
        }
    }
}