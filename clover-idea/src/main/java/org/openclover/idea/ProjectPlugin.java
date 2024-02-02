package org.openclover.idea;

import com.atlassian.clover.Logger;
import com.atlassian.clover.cfg.Interval;
import org.openclover.idea.build.CloverCompiler;
import org.openclover.idea.build.CloverLibraryInjector;
import org.openclover.idea.build.ProjectRebuilder;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.config.IdeaXmlConfigConstants;
import org.openclover.idea.content.ContentManager;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.coverage.DefaultCoverageManager;
import org.openclover.idea.coverage.EventListenerInstallator;
import org.openclover.idea.coverage.monitor.ProcessExitDatabaseMonitor;
import org.openclover.idea.coverage.monitor.PropertyCoverageMonitor;
import org.openclover.idea.coverage.monitor.ThreadedDatabaseMonitor;
import org.openclover.idea.feature.CloverFeatures;
import org.openclover.idea.feature.ConfigPropertyCategory;
import org.openclover.idea.feature.FeatureEvent;
import org.openclover.idea.feature.FeatureListener;
import org.openclover.idea.feature.FeatureManager;
import org.openclover.idea.feature.FeatureTreeNode;
import org.openclover.idea.testexplorer.TestRunExplorerToolWindow;
import org.openclover.idea.util.ProjectUtil;
import org.openclover.idea.util.jdom.JDOMExternUtil;
import com.atlassian.clover.util.Path;
import com.intellij.ProjectTopics;
import com.intellij.execution.ExecutionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@State(name = IdeaXmlConfigConstants.WORKSPACE_FILE_CLOVER_COMPONENT_NAME,
        storages = {@Storage(id = "workspace", file = "$WORKSPACE_FILE$")})
public class ProjectPlugin implements IProjectPlugin, ProjectComponent, ModuleRootListener,
        PersistentStateComponent<Element> {

    private static final Logger LOG = Logger.getInstance("CLOVER:ProjectPlugin");

    private final Project project;
    private IdeaCloverConfig config;

    private ContentManager nManager;
    private CoverageManager cManager;
    private FeatureManager fManager;
    private ThreadedDatabaseMonitor dbMonitor;
    private CloverCompiler compiler;

    private boolean initialized;

    public ProjectPlugin(final Project project) {
        this.project = project;
        config = IdeaCloverConfig.fromProject(this.project);

        StartupManager.getInstance(project).runWhenProjectIsInitialized(this::projectPostStartup);
    }

    /**
     * Get access to the ProjectPlugin instances loaded by the IDE.<p>
     *
     * @param proj project instance
     * @return per-project instance of this plugin.
     */
    public static IProjectPlugin getPlugin(@NotNull Project proj) {
        return proj.getComponent(IProjectPlugin.class);
    }

    /**
     * Utility method to retrieve ProjectPlugin instance from an AnActionEvent.<p>
     *
     * @param event UI event to retrieve Project from.
     * @return ProjectPlugin instance or null when event contains no Project reference.
     * @see #getPlugin(com.intellij.openapi.project.Project)
     */
    @Nullable
    public static IProjectPlugin getPlugin(AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        return project == null ? null : getPlugin(project);
    }

    void projectPostStartup() {
        final CloverLibraryInjector cloverLibraryInjector = new CloverLibraryInjector(project);
        cloverLibraryInjector.updateModulesDependencies();     // update dependencies on project open
        config.addConfigChangeListener(cloverLibraryInjector); // library manager is added before project rebuilder

        final ProjectRebuilder projectRebuilder = ProjectRebuilder.getInstance(project);
        config.addConfigChangeListener(projectRebuilder);

        // needs to be before dbMonitor or database will be already flushed

        CloverToolWindow.getInstance(project).register();
        TestRunExplorerToolWindow.getInstance(project).register();

        // attach to topic with notifications about file editor changes (open / close file events)
        EventListenerInstallator.install(project, FileEditorManagerListener.FILE_EDITOR_MANAGER, getContentManager());

        // support for auto-refresh functionality.
        dbMonitor = new ThreadedDatabaseMonitor();
        dbMonitor.setCoverageManager(getCoverageManager());
        dbMonitor.setMonitorInterval(config.getAutoRefreshInterval());

        final FeatureManager featureManager = getFeatureManager();
        featureManager.addFeatureListener(CloverFeatures.CLOVER_REFRESH, dbMonitor);
        featureManager.addFeatureListener(CloverFeatures.CLOVER_BUILDING, projectRebuilder);

        config.addConfigChangeListener(dbMonitor);

        if (featureManager.isFeatureEnabled(CloverFeatures.CLOVER_REFRESH)) {
            dbMonitor.start();
        }
        new ProjectEnabledListener().register(featureManager);

        compiler = new CloverCompiler(project);
        CompilerManager.getInstance(project).addCompiler(compiler);

        EventListenerInstallator.install(project, ProjectTopics.PROJECT_ROOTS, this);

        initialized = true;

        final ProcessExitDatabaseMonitor executionListener =
                new ProcessExitDatabaseMonitor(getPlugin(project).getCoverageManager(), getPlugin(project).getConfig());
        EventListenerInstallator.install(project, ExecutionManager.EXECUTION_TOPIC, executionListener);

        if (config.isAutoRefresh()) {
            getCoverageManager().reload();
        }
    }

    /**
     * @see ProjectComponent#projectOpened()
     */
    @Override
    public void projectOpened() {
    }

    /**
     * @see ProjectComponent#projectClosed()
     */
    @Override
    public void projectClosed() {
        if (!initialized) {
            return;
        }

        CompilerManager.getInstance(project).removeCompiler(compiler);
        CloverToolWindow.getInstance(project).cleanup();
        TestRunExplorerToolWindow.getInstance(project).unregister();
        compiler.cleanup();
        dbMonitor.cleanup();
        nManager.cleanup();
        cManager.cleanup();
    }

    public ContentManager getContentManager() {
        if (nManager == null) {
            nManager = new ContentManager(project);
            nManager.init();
        }
        return nManager;
    }

    /**
     * Get the clover configuration for this project.
     *
     * @return current config instance
     */
    @Override
    public IdeaCloverConfig getConfig() {
        // returning a reference, not a copy, so that CloverProjectConfigurable could modify it directly
        return config;
    }

    @Override
    public boolean isEnabled() {
        return getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER);
    }

    private static Path getProjectSourcePath(Project project) {
        Path path = new Path();
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        for (VirtualFile root : roots) {
            path.append(root.getPath());
        }
        return path;
    }


    /**
     * Get the CoverageManager for this project.
     *
     * @return existing or newly created instance of CoverageManager.
     */
    @Override
    public CoverageManager getCoverageManager() {
        if (cManager == null) {
            String initString = config.getInitString();
            cManager = new DefaultCoverageManager(project, initString);
            cManager.setSpan(new Interval(config.getSpan()).getValueInMillis());
            cManager.setContextFilter(config.getContextFilterSpec());
            cManager.setSourcePath(getProjectSourcePath(project));
            if (ApplicationManager.getApplication().isDispatchThread()) {
                cManager.reload();
            } else {
                ApplicationManager.getApplication().invokeLater(() -> cManager.reload());
            }

            PropertyCoverageMonitor monitor = new PropertyCoverageMonitor(config);
            monitor.setCoverageManager(cManager);
            monitor.start();
        }
        return cManager;
    }

    @Override
    public FeatureManager getFeatureManager() {
        if (fManager == null) {

            fManager = new FeatureManager();

            FeatureTreeNode root = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.CLOVER, IdeaCloverConfig.ENABLED, config));
            FeatureTreeNode reporting = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.REPORTING, IdeaCloverConfig.VIEW_COVERAGE, config));
            FeatureTreeNode building = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.BUILDING, IdeaCloverConfig.BUILD_WITH_CLOVER, config));
            FeatureTreeNode tooltip = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.TOOLTIPS, IdeaCloverConfig.SHOW_TOOLTIPS, config));
            FeatureTreeNode inline = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.INLINE, IdeaCloverConfig.SHOW_INLINE, config));
            FeatureTreeNode gutter = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.GUTTER, IdeaCloverConfig.SHOW_GUTTER, config));
            FeatureTreeNode errorMarks = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.ERRORMARKS, IdeaCloverConfig.SHOW_ERROR_MARKS, config));

            FeatureTreeNode refresh = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.REFRESH, IdeaCloverConfig.PERIODIC_REFRESH, config));
            FeatureTreeNode iconDecoration = new FeatureTreeNode(new ConfigPropertyCategory(CloverFeatures.ICON_DECORATION, IdeaCloverConfig.VIEW_INCLUDE_ANNOTATION, config));

            root.addChild(reporting);
            root.addChild(building);
            root.addChild(refresh);
            root.addChild(iconDecoration);
            reporting.addChild(tooltip);
            reporting.addChild(inline);
            reporting.addChild(gutter);
            reporting.addChild(errorMarks);

            fManager.registerFeatureTree(root);
        }
        return fManager;
    }

    /**
     * The ProjectWorkspace represents the directory in which clover can write files associated with this components
     * project.
     *
     * @return a file representing the workspace directory.
     */
    public File getProjectWorkspace() {
        return ProjectUtil.getProjectWorkspace(project);
    }

    //---( BaseComponent interface implementation )---

    @Override
    @NotNull
    public String getComponentName() {
        return "CloverPlugin";
    }

    /**
     * Initialise the Project Component.
     */
    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
        // refactor CloverProjectPlugin.checkEnabled when you put anything in here
    }

    // ModuleRootListener

    @Override
    public void beforeRootsChange(ModuleRootEvent moduleRootEvent) {
    }

    @Override
    public void rootsChanged(ModuleRootEvent moduleRootEvent) {
        //todo : err,um thread safety??
        if (cManager != null) {
            LOG.info("rootsChanged - refreshing coverage manager");
            cManager.setSourcePath(getProjectSourcePath(project));
        }
    }

    // PersistentStateComponent

    @Override
    public Element getState() {
        final Element element = new Element("element");
        try {
            JDOMExternUtil.writeTo(element, config);
            config.markDirty(false);
        } catch (Exception e) {
            LOG.error(e);
        }
        return element;
    }

    @Override
    public void loadState(Element element) {
        try {
            JDOMExternUtil.readTo(element, config);
        } catch (Exception e) {
            LOG.warn("problem reading config", e);
        }
        config.notifyListeners();
        config.markDirty(false);
    }
}

/**
 * Automatically enables "clover-building" and "clover-reporting" when plugin changes scope from disabled to enabled.
 */
class ProjectEnabledListener implements FeatureListener {
    private FeatureManager featureManager;

    void register(final FeatureManager featureManager) {
        this.featureManager = featureManager;
        featureManager.addFeatureListener(CloverFeatures.CLOVER, this);
    }

    @Override
    public void featureStateChanged(FeatureEvent evt) {
        if (evt.isEnabled()) {
            featureManager.setCategoryEnabled(CloverFeatures.CLOVER_BUILDING, true);
            featureManager.setCategoryEnabled(CloverFeatures.CLOVER_REPORTING, true);
        }
    }
}

