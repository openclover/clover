package org.openclover.eclipse.core.projects;

import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cfg.instr.java.SourceLevel;
import com.atlassian.clover.instr.tests.AggregateTestDetector;
import com.atlassian.clover.instr.tests.AndStrategy;
import com.atlassian.clover.instr.tests.AntPatternTestDetectorFilter;
import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.recorder.PerTestCoverageStrategy;
import com.atlassian.clover.context.ContextSet;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.PluginVersionInfo;
import org.openclover.eclipse.core.exclusion.DecorationPreferenceChangeListener;
import org.openclover.eclipse.core.projects.builder.BuildCoordinator;
import org.openclover.eclipse.core.projects.builder.Markers;
import org.openclover.eclipse.core.projects.builder.NoJavaCloverBuilder;
import org.openclover.eclipse.core.projects.builder.PathUtils;
import org.openclover.eclipse.core.projects.builder.PostJavaCloverBuilder;
import org.openclover.eclipse.core.projects.builder.PreJavaCloverBuilder;
import org.openclover.eclipse.core.projects.builder.ProjectPathMap;
import org.openclover.eclipse.core.projects.model.ClosedDatabaseModel;
import org.openclover.eclipse.core.projects.model.CoverageClearerModelDecorator;
import org.openclover.eclipse.core.projects.model.CoverageModelChangeEvent;
import org.openclover.eclipse.core.projects.model.DatabaseModel;
import org.openclover.eclipse.core.projects.model.DatabasePostLoadDecorator;
import org.openclover.eclipse.core.projects.model.DatabasePreLoadDecorator;
import org.openclover.eclipse.core.projects.model.FolderAwareTestDetectorFilter;
import org.openclover.eclipse.core.projects.model.FoldersAwareTestFilter;
import org.openclover.eclipse.core.projects.model.ModelOperation;
import org.openclover.eclipse.core.projects.model.UnloadedDatabaseModel;
import org.openclover.eclipse.core.projects.model.WorkingSetHasMetricsFilter;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static clover.com.google.common.collect.Lists.newArrayList;

public class CloverProject extends BaseNature {
    public static final String ID = CloverPlugin.ID + ".clovernature";
    /* id of builder no longer in use */

    private static final QualifiedName CLOVER_VERSION_PROPERTY = new QualifiedName(CloverPlugin.ID, "Version");
    private static final QualifiedName FILES_BEING_COMPILED = new QualifiedName(CloverPlugin.ID, "FilesBeingCompiled");
    private static final QualifiedName LAST_CLEAN_BUILD_STAMP = new QualifiedName(CloverPlugin.ID, "last_full_build_stamp");

    private static final String DEFAULT_CLOVER_DIR = ".clover";
    private static final String COVERAGE_DB_FILE = "coverage.db";

    private static final String CLOVER_JAR_MISSING_MSG =
            "Your instrumented application could not find the Clover classes necessary to record coverage. " +
                    "Please use the 'Run with Clover' option to correct this.";

    /**
     * Current coverage model - may be unloaded, loading, or loaded.
     * Marked as volatile since we use Eclipse locks, not 'synchronized' and need
     * cross-thread visibility for this member.
     */
    private volatile DatabaseModel model = new UnloadedDatabaseModel(this);

    /** Lock object - used instead of 'synchronized' as Eclipse has deadlock detection and resolution baked in */
    private final ILock modelMutex = Job.getJobManager().newLock();
    private final BuildCoordinator buildCoordinator;
    private ProjectSettings settings;
    private static final IPreferenceChangeListener EXCLUSION_PREFERENCE_CHANGE_LISTENER = new DecorationPreferenceChangeListener();
    private static final int JOIN_SLEEP_MS = 1000;

    public CloverProject() {
        buildCoordinator = new BuildCoordinator(this);
    }

    /**
     * Remove the Java builder, add the Clover builder and add clover-eclipse.jar to
     * the classpath
     */
    @Override
    public void configure() throws CoreException {
        ensureClasspathEntryAdded(getCloverJar());
        ensureBuildersPresent(project);
        ensureLastBuildStamped();
    }

    public static void ensureBuildersPresent(IProject project) throws CoreException {
        IProjectDescription description = project.getDescription();
        List commands = newArrayList(project.getDescription().getBuildSpec());

        commands = ensureBuilderAddedBefore(description, commands, JavaCore.BUILDER_ID, PreJavaCloverBuilder.ID, NoJavaCloverBuilder.ID);
        commands = ensureBuilderAddedAfter(description, commands, JavaCore.BUILDER_ID, PostJavaCloverBuilder.ID, NoJavaCloverBuilder.ID);

        description.setBuildSpec((ICommand[]) commands.toArray(new ICommand[commands.size()]));
        project.setDescription(description, null);
    }

    public static boolean buildersPresent(IProject project) throws CoreException {
        List builders = newArrayList(project.getDescription().getBuildSpec());
        return
            (builderPresent(builders, PreJavaCloverBuilder.ID)
            && builderPresent(builders, PostJavaCloverBuilder.ID))
            || builderPresent(builders, NoJavaCloverBuilder.ID);
    }

    public static boolean builderPresent(IProject project, String builderId) throws CoreException {
        return builderPresent(newArrayList(project.getDescription().getBuildSpec()), builderId);
    }

    private static boolean builderPresent(List builders, String builderId) {
        for (int i = 0; i < builders.size(); i++) {
            if (((ICommand)builders.get(i)).getBuilderName().equals(builderId)) {
                return true;
            }
        }
        return false;
    }

    private void ensureLastBuildStamped() {
        try {
            if (project.getPersistentProperty(LAST_CLEAN_BUILD_STAMP) != null) {
                project.setPersistentProperty(LAST_CLEAN_BUILD_STAMP, Long.toString(System.currentTimeMillis()));
            }
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to set initial time stamp for project", e);
        }
    }

    /**
     * Remove the Clover builder, re-add the Java builder and add clover-eclipse.jar to
     * the classpath
     */
    @Override
    public void deconfigure() throws CoreException {
        Markers.deleteMarkersFor(project, Markers.ID);
        ensureBuilderRemoved(project, PreJavaCloverBuilder.ID);
        ensureBuilderRemoved(project, PostJavaCloverBuilder.ID);
        ensureBuilderRemoved(project, NoJavaCloverBuilder.ID);
        ensureClasspathEntryAbsent(getCloverJar());
        settings.removeListener(EXCLUSION_PREFERENCE_CHANGE_LISTENER);
    }

    @Override
    public void setProject(final IProject project) {
        super.setProject(project);
        settings = new ProjectSettings(project);
        settings.upgrade();
        settings.addListener(EXCLUSION_PREFERENCE_CHANGE_LISTENER);
        ensureWorkingDirCreated();
        ensureLastBuildStamped();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
            new IResourceChangeListener() {
                @Override
                public void resourceChanged(IResourceChangeEvent event) {
                    if (event.getResource().equals(project) && project.isOpen()) {
                        CloverPlugin.logVerbose("A Clover project is closing");
                        setModel(new ClosedDatabaseModel(CloverProject.this, CoverageModelChangeEvent.CLOSE(CloverProject.this)));
                    }
                }
            }, IResourceChangeEvent.PRE_CLOSE);
    }

    public void ensureWorkingDirCreated() {
        IFolder workingDir = getCloverWorkingDir();
        if (!workingDir.exists()) {
            try {
                PathUtils.makeDerivedFoldersFor(workingDir);
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to create Clover working directory " + workingDir.getLocation(), e);
            }
        }
    }

    private IClasspathEntry getCloverJar() {
        return JavaCore.newVariableEntry(CloverPlugin.CLOVER_RUNTIME_VARIABLE, null, null);
    }

    public void refreshCloverWorkingDir(IProgressMonitor monitor) throws CoreException {
        getCloverWorkingDir().refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }

    /**
     * @return true if this project is Clover-enabled?
     */
    public static boolean isAppliedTo(IProject project) throws CoreException {
        return project != null && project.exists() && project.isOpen() && project.isAccessible() && project.getNature(ID) != null;
    }

    /**
     * @return true if this Java project is Clover-enabled?
     */
    public static boolean isAppliedTo(IJavaProject project) throws CoreException {
        return project != null && project.exists() && project.getProject().isOpen() && project.getProject().isAccessible() && project.getProject().getNature(ID) != null;
    }

    /**
     * Toggle Clover-enablement on this project
     */
    public static void toggle(IJavaProject project) throws CoreException {
        boolean isApplied = isAppliedTo(project);

        // Get the description.
        IProjectDescription description;
        try {
            description = project.getProject().getDescription();
        }
        catch (CoreException e) {
            CloverPlugin.logError("Error getting description from project", e);
            throw e;
        }

        String[] oldIds = description.getNatureIds();
        List<String> newIds = newArrayList(oldIds); // copy

        if (isApplied) {
            CloverPlugin.logVerbose("Removing nature" + ID);
            newIds.remove(ID);
        } else {
            CloverPlugin.logVerbose("Adding nature " + ID);
            newIds.add(ID);
        }

        description.setNatureIds(newIds.toArray(new String[newIds.size()]));

        if (isApplied) {
            try {
                getFor(project).closeCoverage();
            }
            catch (CoreException e) {
                CloverPlugin.logError("Error closing coverage model", e);
            }
        }

        // Save the description.
        try {
            project.getProject().setDescription(description, null);
            CloverPlugin.getInstance().getCoverageMonitor().fireCoverageChange();
        }
        catch (CoreException e) {
            CloverPlugin.logError("Error setting description for project, attempting to undo", e);
            try {
                description.setNatureIds(oldIds);
                project.getProject().setDescription(description, null);
            } catch (CoreException e2) {
                CloverPlugin.logError("Error undoing changes to description for project, most likely related to the previous error", e2);
            }
            throw e;
        }
    }

    public static CloverProject getFor(IProject project) throws CoreException {
        return isAppliedTo(project) ? (CloverProject)project.getNature(ID) : null;
    }

    public static CloverProject getFor(IJavaProject project) throws CoreException {
        return isAppliedTo(project) ? (CloverProject)project.getProject().getNature(ID) : null;
    }

    public IFolder getCloverWorkingDir() {
        return getProject().getFolder(DEFAULT_CLOVER_DIR);
    }

    /** @return the coverage db file based on current project settings */
    public File getRegistryFile() {
        if (settings.isInitStringDefault()) {
            return getCloverWorkingDir().getFile(COVERAGE_DB_FILE).getLocation().toFile();
        } else {
            final String initString = settings.getInitString();
            if (settings.isInitStringProjectRelative()) {
                return new File(getProject().getLocation().toFile(), initString);
            } else {
                return new File(initString);
            }
        }
    }

    /** @return the coverage db workspace resource or null if not in the workspace */
    public IFile getCoverageDbIFile() {
        if (settings.isInitStringDefault()) {
            return getCloverWorkingDir().getFile(COVERAGE_DB_FILE);
        } else {
            final String initString = settings.getInitString();
            if (settings.isInitStringProjectRelative()) {
                try {
                    return getProject().getFile(initString);
                } catch (Exception e) {
                    //Exception may happen if this file doesn't resolve to something in the project (../coverage.db)
                }
            }
            return null;
        }
    }

    public String getName() {
        return project.getName();
    }

    public JavaInstrumentationConfig newInsturmentationConfig() throws CoreException{
        JavaInstrumentationConfig config = new JavaInstrumentationConfig();
        config.setInitstring(deriveInitString());
        config.setProjectName(project.getName());
        config.setEncoding(getProject().getDefaultCharset());
        config.setDefaultBaseDir(getProject().getLocation().toFile());
        config.setSourceLevel(SourceLevel.fromString(getJavaProject().getOption(JavaCore.COMPILER_SOURCE, true)));
        config.setFlushPolicy(settings.getFlushPolicy());
        config.setFlushInterval(settings.getFlushInterval());
        config.setFullyQualifyJavaLang(settings.shouldQualifyJavaLang());
        config.setTestDetector(getTestDetector(config.getTestDetector())); // retrieve default detector first
        config.setInstrLevelStrategy(settings.getInstrumentationLevel().name());
        config.setInstrumentLambda(settings.getInstrumentLambda());
        config.setClassNotFoundMsg(CLOVER_JAR_MISSING_MSG);
        return config;
    }

    private TestDetector getTestDetector(TestDetector defaultTestDetector) {
        switch (settings.getTestSourceFolders()) {
            case ProjectSettings.Values.NO_TEST_FOLDERS:
                return new TestDetector() {
                    @Override
                    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
                        return false;
                    }

                    @Override
                    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
                        return false;
                    }
                }; // do not detect any tests
            case ProjectSettings.Values.SELECTED_FOLDERS:
                AggregateTestDetector atd = new AggregateTestDetector(new AndStrategy());
                atd.addDetector(new FolderAwareTestDetectorFilter(this, getSettings().getSelectedTestFolders()));
                atd.addDetector(defaultTestDetector);
                return atd;
            default:
                atd = new AggregateTestDetector(new AndStrategy());
                atd.addDetector(new AntPatternTestDetectorFilter(project.getLocation().toFile(),
                                                                 getSettings().calculateTestIncludeFilter(),
                                                                 getSettings().calculateTestExcludeFilter()));
                atd.addDetector(defaultTestDetector);
                return atd;
        }
    }

    public String deriveInitString() {
        if (settings.isInitStringDefault()) {
            return getCloverWorkingDir().getLocation().append(COVERAGE_DB_FILE).toPortableString();
        } else {
            final String initString = settings.getInitString();
            if (settings.isInitStringProjectRelative()) {
                return new File(getProject().getLocation().toFile(), initString).getAbsolutePath();
            } else {
                return initString;
            }
        }
    }

    public <T> T onPinnedModel(ModelOperation<T> operation) {
        modelMutex.acquire();
        try {
            return operation.run(model);
        } finally {
            modelMutex.release();
        }
    }

    public DatabaseModel getModel() {
        return onPinnedModel(new ModelOperation<DatabaseModel>() {
            @Override
            public DatabaseModel run(DatabaseModel model) {
                if (!model.isLoaded()) {
                    model.loadDbAndCoverage(CoverageModelChangeEvent.IDLY(CloverProject.this));
                }
                return model;
            }
        });
    }

    public void setModel(final DatabaseModel update) {
        onPinnedModel(new ModelOperation<Void>() {
            @Override
            public Void run(DatabaseModel model) {
                maybeSetModel(model, update);
                return null;
            }
        });
    }

    /**
     * Compares the current model with the expected model and updates its value if true.
     * Should only be used when an asynchronous job relating to a mdoel state finishes
     * and the job isn't certain the original state is still in play.
     *
     * @return true if the model was set, false otherwise
     */
    public boolean compareAndSetModel(final DatabaseModel expected, final DatabaseModel update) {
        return onPinnedModel(new ModelOperation<Boolean>() {
            @Override
            public Boolean run(DatabaseModel model) {
                return maybeSetModel(expected, update);
            }
        });
    }

    private boolean maybeSetModel(DatabaseModel expected, DatabaseModel update) {
        if (model == expected) {
            CloverPlugin.logVerbose("De-activating coverage model " + expected);
            try {
                model.onDeactication(update);
            } catch (Throwable t) {
                CloverPlugin.logError("Failed to deactivate model " + model, t);
            }
            CloverPlugin.logVerbose("Switching coverage model from " + expected + " to " + update);
            model = update;
            CloverPlugin.logVerbose("Activating coverage model " + update);
            try {
                model.onActivation(expected);
            } catch (Throwable t) {
                CloverPlugin.logDebug("Failed to activate model " + model, t);
            }
            CloverPlugin.getInstance().getCoverageMonitor().fireCoverageChange(this, expected, update);
            return true;
        } else {
            return false;
        }
    }

    public CloverDatabase joinOnLoad(IProgressMonitor monitor) {
        CloverDatabase database = null;
        do {
            final DatabaseModel loadedOrLoadingModel =
                onPinnedModel(new ModelOperation<DatabaseModel>() {
                    @Override
                    public DatabaseModel run(DatabaseModel model) {
                        if (model.isLoading() || model.isLoaded()) {
                            return model;
                        } else {
                            model.loadDbAndCoverage(new CoverageModelChangeEvent(CloverProject.this, "Load-join", false));
                            //*this* model is not loading, the next time around the loop, it may be. Return null to shortcut evaluation this round
                            return null;
                         }
                    }
                });
            if (loadedOrLoadingModel != null) {
                if (loadedOrLoadingModel.isLoading()) {
                    //Loading may be blocked because of scheduling rules
                    //We bring forward loading to allow compilation to proceed
                    database = loadedOrLoadingModel.forcePrematureLoad(monitor);
                } else if (loadedOrLoadingModel.isLoaded()) {
                    database = loadedOrLoadingModel.getDatabase();
                }
            }

            if (database == null) {
                try {
                    Thread.sleep(JOIN_SLEEP_MS);
                } catch (InterruptedException e) {
                    //Ignore
                }
            }
        } while(database == null && !monitor.isCanceled());

        return database;
    }

    public static void refreshAllModels(boolean forceModelReload, boolean forceCoverageReoload) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        for (IProject project : projects) {
            try {
                if (CloverProject.isAppliedTo(project)) {
                    CloverProject.getFor(project).refreshModel(forceModelReload, forceCoverageReoload);
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Error while refreshing coverage for project " + project.getName(), e);
            }
        }
    }

    public void refreshModel(boolean forceModelReload, boolean forceCoverageReload) {
        refreshModel(
            forceModelReload,
            forceCoverageReload,
            new CoverageModelChangeEvent(
                this,
                "Reloading coverage",
                forceModelReload));
    }

    public void refreshModel(final boolean forceModelReload, final boolean forceCoverageReload, final CoverageModelChangeEvent changeEvent) {
        onPinnedModel(new ModelOperation<Void>() {
            @Override
            public Void run(DatabaseModel model) {
                if ((forceModelReload || model.isRegistryOfDate()) && !model.isLoading()) {
                    model.loadDbAndCoverage(changeEvent);
                } else if ((forceCoverageReload || model.isCoverageOutOfDate()) && !model.isLoading()) {
                    model.refreshCoverage(changeEvent);
                }
                return null;
            }
        });
    }

    public void clearCoverage() {
        onPinnedModel(new ModelOperation<Void>() {
            @Override
            public Void run(DatabaseModel model) {
                model.loadDbAndCoverage(
                    new CoverageModelChangeEvent(
                        "Clearing coverage",
                        true,
                        new DatabasePreLoadDecorator[] { new CoverageClearerModelDecorator() },
                        DatabasePostLoadDecorator.NONE));
                return null;
            }
        });
    }

    private void closeCoverage() {
        onPinnedModel(new ModelOperation<Void>() {
            @Override
            public Void run(DatabaseModel model) {
                model.close(CoverageModelChangeEvent.CLOSE(CloverProject.this));
                return null;
            }
        });
    }

    public IFolder getReportDir() {
        return getCloverWorkingDir().getFolder("report");
    }

    /** Perform exclusive operation on working dir */
    public void runOnWorkingDir(Callable callable) throws Exception {
        IResourceRuleFactory ruleFactory =
            ResourcesPlugin.getWorkspace().getRuleFactory();
        ISchedulingRule rule = ruleFactory.createRule(getCloverWorkingDir());
        try {
            Job.getJobManager().beginRule(rule, null);

            callable.call();

        } finally {
            Job.getJobManager().endRule(rule);
        }
    }

    public ISchedulingRule getCloverWorkingDirSchedulingRule() {
        return getCloverWorkingDir();
    }

    public ProjectPathMap getPathMap() throws CoreException {
        return new ProjectPathMap(getJavaProject());
    }

    public void addInstrumentationFailure(IFile originalFile) throws CoreException {
        List failures = (List)getProject().getSessionProperty(new QualifiedName(CloverPlugin.ID, "instrumentation.failures"));
        if (failures == null) {
            failures = newArrayList();
            getProject().setSessionProperty(new QualifiedName(CloverPlugin.ID, "instrumentation.failures"), failures);
        }
        failures.add(originalFile);
    }

    public void clearInstrumentationFailures() throws CoreException {
        getProject().setSessionProperty(new QualifiedName(CloverPlugin.ID, "instrumentation.failures"), new ArrayList());
    }

    public List getInstrumentationFailures() throws CoreException {
        List failures = (List)getProject().getSessionProperty(new QualifiedName(CloverPlugin.ID, "instrumentation.failures"));
        if (failures == null) {
            failures = Collections.emptyList();
        }
        return failures;
    }

    public BuildCoordinator getBuildCoordinator() throws CoreException {
        return buildCoordinator;
    }

    public ProjectSettings getSettings() {
        return settings;
    }

    public static Version getLastVersionStamp(IProject project) throws CoreException {
        try {
            String versionString = project.getPersistentProperty(CLOVER_VERSION_PROPERTY);
            //alpha 1 had no concept of Clover version, so if property is null
            //it's likely that we're looking at an alpha 1 or alpha 1_1 project
            return versionString == null ? Version.V2_0_0_A1_1 : new Version(versionString);
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to query Clover version last applied to project", e);
            throw e;
        }
    }

    public static void stampWithCurrentVersion(IProject project) throws CoreException {
        project.setPersistentProperty(CLOVER_VERSION_PROPERTY, Version.CURRENT_VERSION.toString());
    }

    public static boolean toggleWithUserFeedback(Shell shell, IProject project) {
        return toggleWithUserFeedback(shell, JavaCore.create(project));
    }

    public static boolean toggleWithUserFeedback(final Shell shell, IJavaProject javaProject) {
        final boolean[] isProjectCloverEnabled = new boolean[] {false};
        try {
            isProjectCloverEnabled[0] = CloverProject.isAppliedTo(javaProject);
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to determine if project is Clover-enabled", e);
        }
        try {
            CloverProject.toggle(javaProject);
            return true;
        } catch (Throwable t) {
            CloverPlugin.logError("Error toggling Clover support", t);
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    MessageDialog.openError(
                        shell,
                        "Unable to " + (isProjectCloverEnabled[0] ? "disable" : "enable") + " Clover",
                        "An error occurred while " + (isProjectCloverEnabled[0] ? "disabling" : "enabling") + " Clover on this project.\n" +
                        "Please check that Eclipse has permission to write to the project directory, that\n" +
                        "any Team plugins you use allow Eclipse to save the project file and then try\n" +
                        "again.");
                }
            });
            return false;
        }
    }

    public IFolder getInstrumentationOutputRootDir() throws CoreException {
        if (!getSettings().isOutputRootSameAsProject()) {
            IPath path = project.getFullPath().append(getSettings().getOutputRoot());
            if (ResourcesPlugin.getWorkspace().validatePath(path.toString(), IResource.FOLDER).getCode() == IStatus.OK) {
                return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
            }
        }
        return null;
    }

    public CloverProject[] getDependencies() throws CoreException {
        IProject[] referenced = getProject().getReferencedProjects();
        List<CloverProject> projects = new ArrayList<>(referenced.length);
        for (IProject project : referenced) {
            if (CloverProject.isAppliedTo(project)) {
                projects.add(CloverProject.getFor(project));
            }
        }
        return projects.toArray(new CloverProject[projects.size()]);
    }

    public long getLastCleanBuildStamp() {
        return getLastCleanBuildStamp(project);
    }

    public static long getLastCleanBuildStamp(IProject project) {
        try {
            String value = project.getPersistentProperty(LAST_CLEAN_BUILD_STAMP);
            return value == null ? 0L : Long.parseLong(value);
        } catch (CoreException e) {
            CloverPlugin.logError("Invalid build stamp", e);
            long now = System.currentTimeMillis();
            setLastCleanBuildStamp(project, now);
            return now;
        }
    }

    public void setLastCleanBuildStamp(long timeStamp) {
        setLastCleanBuildStamp(project, timeStamp);
    }

    private static void setLastCleanBuildStamp(IProject project, long timeStamp) {
        try {
            project.setPersistentProperty(LAST_CLEAN_BUILD_STAMP, Long.toString(timeStamp));
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to set build stamp for project", e);
        }
    }

    public IPath getWorkingPath() {
        return project.getWorkingLocation(CloverPlugin.getInstance().getBundle().getSymbolicName());
    }

    public void setFilesNeedingCloverCompile(Set<IFile> files) throws CoreException {
        project.setSessionProperty(FILES_BEING_COMPILED, files);
    }

    public void clearFilesNeedingCloverCompile() throws CoreException {
        project.setSessionProperty(FILES_BEING_COMPILED, null);
    }

    @SuppressWarnings("unchecked")
    public Set<IFile> getFilesNeedingCloverCompile() throws CoreException {
        return (Set<IFile>)project.getSessionProperty(FILES_BEING_COMPILED);
    }

    public boolean okayToRebuild(Shell shell) {
        return MessageDialog.openQuestion(
            shell,
            "Clover compilation settings changed",
            "Clover compilation settings have changed. To take effect you should rebuild your project.\n\nRebuild your project now?");
    }

    public CloverDatabase newEmptyDatabase() {
        return newEmptyDatabase(settings.getContextFilter());
    }
    
    public CloverDatabase newEmptyDatabase(ContextSet contextFilter) {
        return
            new CloverDatabase(
                getRegistryFile(),
                false,
                project.getName(),
                contextFilter,
                newCoverageDataSpec(null));
    }

    public HasMetricsFilter newIncludeFilter() {
        return
            CloverPlugin.getInstance().isInWorkingSetMode()
                ? new WorkingSetHasMetricsFilter(this)
                : HasMetricsFilter.ACCEPT_ALL;
    }

    public CoverageDataSpec newCoverageDataSpec(CloverDatabase database) {
        return new CoverageDataSpec(
            FoldersAwareTestFilter.getFor(this),
            settings.calcEffectiveSpanMS(database),
            shouldDeleteUnusedCoverage(), true, true,
            CloverPlugin.getInstance().getInstallationSettings().isTrackingPerTestCoverage(),
            CloverPlugin.getInstance().getInstallationSettings().isPerTestCoverageInMemory()
                ? PerTestCoverageStrategy.IN_MEMORY
                : PerTestCoverageStrategy.SAMPLING);
    }

    public boolean shouldDeleteUnusedCoverage() {
        //Only delete coverage if we are generating it through compilation
        //otherwise Ant or Maven tools integrated with Eclipse may have all their hard work undone
        //within 2s/5s/10s/20s of finishing it
        return settings.isInstrumentationEnabled();
    }

    public interface Callable {
        void call() throws Exception;
    }

    public static class Version implements Comparable {
        public static final Version V2_0_0_A1 = new Version("2_0_0_a1"); //released externally
        public static final Version V2_0_0_A1_1 = new Version("2_0_0_a1_1"); //released externally
        public static final Version V2_0_0_A2 = new Version("2_0_0_a2"); //never released externally
        public static final Version V2_0_0_B1 = new Version("2_0_0_b1"); //released externally
        public static final Version V2_0_0_B2 = new Version("2_0_0_b2"); //released externally
        public static final Version CURRENT_VERSION = new Version(PluginVersionInfo.RELEASE_NUM.replace('.', '_'));

        private String label;

        public Version(String label) {
            this.label = label;
        }

        public String toString() {
            return label;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Version version = (Version) o;

            if (!Objects.equals(label, version.label)) return false;

            return true;
        }

        public int hashCode() {
            return (label != null ? label.hashCode() : 0);
        }

        @Override
        public int compareTo(Object other) {
            if (other instanceof Version) {
                return label.compareTo(((Version)other).label);
            } else {
                return -1;
            }
        }
    }

    public void flagStaleRegistryBecause(String message) {
        try {
            project.setPersistentProperty(Markers.STALEDB_PROPERTY_NAME, message);
            Markers.deleteCloverStaleDbMarkers(project.getProject());
            if (message != null) {
                Markers.createCloverStaleDbMarker(project.getProject(), message);
            }
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to set/unset stale registry message", e);
        }
    }
}