package org.openclover.idea.coverage;

import org.openclover.core.CloverDatabase;
import org.openclover.runtime.Logger;
import org.openclover.core.context.ContextSet;
import org.openclover.core.context.ContextStore;
import org.openclover.core.context.NamedContext;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.idea.IdeaTestFilter;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.util.ModelScope;
import org.openclover.idea.config.regexp.Regexp;
import org.openclover.idea.report.jfc.FileFilter;
import org.openclover.idea.util.tasks.AbstractCancellableTaskDelegate;
import org.openclover.idea.util.tasks.CancellableTaskDelegate;
import org.openclover.idea.util.tasks.ProgressIndicatorAdapter;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.registry.metrics.HasMetricsFilter;
import org.openclover.core.util.CloverUtils;
import org.openclover.core.util.Path;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The CoverageManager provides a managed interface to a Coverage Model. That
 * is, it handles the loading, configuration and notification of changes for
 * and to a coverage model.
 */
public class DefaultCoverageManager implements CoverageManager, AcceptsCoverageTreeModel, ConfigChangeListener {

//    private final Logger LOG = Logger.getInstance("DefaultCoverageManager");

    //---(Cached coverage information)---

    /**
     * cached coverage tree model
     */
    private CoverageTreeModel treeModel;

    //---(Coverage Configuration details)---

    /**
     * the init string of the coverage database
     */
    private String initString;

    /**
     * the span of the coverage database
     */
    private long span;

    /**
     * the context filter of the coverage database
     */
    private String filter = "";

    //---(Coverage Listeners / Coverage Monitors)---

    private final List<CoverageListener> listeners = new CopyOnWriteArrayList<>();
    private final List<CoverageTreeListener> treeListeners = new CopyOnWriteArrayList<>();

    private Path sourcePath;
    private boolean loadPerTestData;

    private Project project;
    private final IdeaCloverConfig cloverConfig;

    private final UpdateTaskScheduler updateTaskScheduler;
    private final HasMetricsFilter.Invertable testFilter;
    private final HasMetricsFilter includeFilter;

    /**
     * @param project    current project
     * @param initString can not be null.
     */
    public DefaultCoverageManager(Project project, String initString) {
        this(project, initString, new FileFilter(project), new IdeaTestFilter(project));
    }

    DefaultCoverageManager(Project project, String initString, HasMetricsFilter includeFilter, HasMetricsFilter.Invertable ideaTestFilter) {
        //To change body of created methods use File | Settings | File Templates.

        //the setter below converts form URL to String, not applicable here
        //noinspection SetterBypass
        this.initString = initString;
        this.project = project;
        cloverConfig = ProjectPlugin.getPlugin(project).getConfig();

        loadPerTestData = cloverConfig.isLoadPerTestData();
        cloverConfig.addConfigChangeListener(this);

        updateTaskScheduler = new UpdateTaskScheduler(project);
        this.testFilter = ideaTestFilter;
        this.includeFilter = includeFilter;
    }

    @Override
    public void cleanup() {
        updateTaskScheduler.cancel();
    }

    @Override
    public synchronized void setInitString(URL coverageUrl) {
        if (coverageUrl == null || coverageUrl.getFile() == null) {
            throw new IllegalArgumentException("Coverage URL can not be 'null'.");
        }

        File f = new File(coverageUrl.getFile());
        initString = f.getAbsolutePath();

        clearCache();
    }

    @Override
    public URL getInitString() {
        try {
            return new File(initString).toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public String getCoverageDatabasePath() {
        return initString;
    }

    @Override
    public void setSpan(long newSpan) {
        if (span != newSpan) {
            span = newSpan;
            clearCache();
        }
    }

    @Override
    public long getSpan() {
        return span;
    }

    @Override
    public void setContextFilter(String newFilter) {
        if (!filter.equals(newFilter)) {
            filter = newFilter;
            clearCache();
        }
    }

    @Override
    public String getContextFilter() {
        return filter;
    }

    private void clearCache() {
        setCoverageTree(null);
    }

    /**
     * Returns the currently loaded coverage database.<p>
     * Asynchronous update might be in progress, so be prepared for out of date/null info here.<p>
     * getCoverage() no longer triggers db reload process - use {@link #reload()}  + listeners to force data load.
     *
     * @return Current database (might be null).
     */
    @Override
    @Nullable
    public CloverDatabase getCoverage() {
        return treeModel != null ? treeModel.getCloverDatabase() : null;
    }

    @Override
    public void setCoverageTree(CoverageTreeModel model) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        synchronized(this) {
            // synchronized for getCoverageTree()
            treeModel = model;
        }
        if (model != null && model.getCloverDatabase() != null) {
            applyCustomContextFilters(model.getCloverDatabase());
        }

        notifyListeners();
    }

    private void applyDataProviderChange() {
        synchronized (this) {
            if (treeModel == null || treeModel.applyIncludePassedTestCoverageOnlyFilter(cloverConfig.isIncludePassedTestCoverageOnly())) {
                return;
            }
        }
        notifyListeners();
    }

    /**
     * @param evt ConfigChangeEvent
     */
    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.INCLUDE_PASSED_TEST_COVERAGE_ONLY)) {
            applyDataProviderChange();
        }
        if (evt.hasPropertyChange(IdeaCloverConfig.LOAD_PER_TEST_DATA)) {
            boolean b = (Boolean) evt.getPropertyChange(IdeaCloverConfig.LOAD_PER_TEST_DATA).getNewValue();
            final CloverDatabase cloverDb = getCoverage();
            final boolean needReload = (!loadPerTestData) && b && cloverDb != null && !cloverDb.getFullModel().hasTestResults();
            loadPerTestData = b;
            if (needReload) {
                //reload only when enabling loadPerTestData and current model does not already contain them 
                loadCoverageData(false);
            }
        }
    }

    private void applyCustomContextFilters(CloverDatabase database) {
        final List<Regexp> configuredRegexps = cloverConfig.getRegexpContexts();
        final ContextStore contextStore = database.getContextStore();

        final StringBuilder sb = new StringBuilder(cloverConfig.getContextFilterSpec());
        String separator = sb.length() == 0 ? "" : ", ";
        boolean needsChange = false;
        for (Regexp configuredRegexp : configuredRegexps) {
            if (configuredRegexp.isEnabled()) {
                NamedContext c = contextStore.getContext(configuredRegexp.getName());
                if (!configuredRegexp.isDifferent(c)) {
                    needsChange = true;
                    sb.append(separator);
                    sb.append(configuredRegexp.getName());
                    separator = ", ";
                }
            }
        }

        if (needsChange) {
            ContextSet filter = database.getContextSet(sb.toString());
            database.getRegistry().getProject().setContextFilter(filter);
        }
    }

    /**
     * Returns the currently loaded coverage database.<p>
     * getCoverageTree() does not trigger db reload - use {@link #reload()} + listeners to force data load.
     *
     * @return Current CoverageTreeModel (might be null).
     */
    @Override
    @Nullable
    public synchronized CoverageTreeModel getCoverageTree() {
        // synchronized for background coverage data load task
        return treeModel;
    }

    @Override
    public void addCoverageListener(CoverageListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Override
    public void removeCoverageListener(CoverageListener l) {
        if (!listeners.remove(l)) {
            // CoverageListener l was not registered as a listener.
        }
    }

    @Override
    public void addCoverageTreeListener(CoverageTreeListener l) {
        if (!treeListeners.contains(l)) {
            treeListeners.add(l);
        }
    }

    @Override
    public void removeCoverageTreeListener(CoverageTreeListener l) {
        if (!treeListeners.remove(l)) {
            // CoverageTreeListener l was not registered as a listener.
        }
    }

    public void notifyListeners() {

        for (CoverageListener listener : listeners) {
            listener.update(getCoverage());
        }

        for (CoverageTreeListener listener : treeListeners) {
            listener.update(getCoverageTree());
        }

    }

    public void reload(boolean flush) {
        // flush any cached data...
        if (flush) {
            clearCache();
        }
        reload();
    }

    @Override
    public boolean canLoadCoverageData() {
        if (updateTaskScheduler.isLoadingCoverage() || updateTaskScheduler.isReloading()) {
            return false;
        }
        CloverDatabase currentDatabase = getCoverage();
        return currentDatabase != null && currentDatabase.isCoverageOutOfDate();
    }

    @Override
    public void loadCoverageData(final boolean forcePerTestData) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (getCoverageTree() == null) {
            reload();
            return;
        }

        final CancellableTaskDelegate coverageLoadTask = new CancellableCoverageLoadTaskDelegate(forcePerTestData);
        updateTaskScheduler.scheduleCoverageLoadTask(coverageLoadTask);
    }

    @Override
    public void lockRegistryForUpdate(Clover2Registry registry) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (treeModel != null) {
            final CloverDatabase currentDatabase = treeModel.getCloverDatabase();
            if (currentDatabase != null && currentDatabase.getRegistry() == registry) {
                treeModel.startRegistryUpdate();
            }
        }
    }

    @Override
    public void releaseUpdatedRegistry(Clover2Registry registry) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        final CloverDatabase currentCoverage = getCoverage();
        if (currentCoverage == null || currentCoverage.getRegistry() != registry) {
            Logger.getInstance().warn("Modified registry is different from the currently loaded one, forcing reload");
            reload();
        } else {
            updateTaskScheduler.restartCurrentTask();
            treeModel.registryUpdated();
            notifyListeners();
        }
    }

    @Override
    public void reload() {
        reload(this);
    }

    void reload(final AcceptsCoverageTreeModel recipient) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        final CoverageTreeModel newModel = new CoverageTreeModel("Project", initString,
                                                                             span, filter, true,
                                                                             sourcePath, cloverConfig.getModelScope(), loadPerTestData,
                                                                             testFilter,
                                                                             includeFilter, project);
        if (newModel.canLoad()) {
            final CancellableTaskDelegate reloadTask = new CancellableReloadTaskDelegate(newModel, recipient);
            updateTaskScheduler.scheduleReloadTask(reloadTask);
        } else {
            setCoverageTree(null);
        }
    }

    @Override
    public synchronized boolean canRefresh() {
        final CloverDatabase database = getCoverage();
        if (database == null) {
            // we can reload if we have an initString that references an existing file
            // but we have not yet constructed a model.
            return (initString != null && initString.length() > 0 && new File(initString).exists());
        } else {
            return database.isOutOfDate();
        }
    }

    /**
     * Delete the coverage database, and all associated coverage recordings.
     *
     * @return true if deletion was successful.
     */
    @Override
    public boolean delete() {

        //NOTE: deleteing the coverage database should trigger an appropriate
        //      system reload.
        if (!CloverUtils.scrubCoverageData(initString, true)) {
            return false;
        }

        // now ensure that the coverage manager is in the correct state.
        reload(true);

        return true;
    }

    @Override
    public boolean clean() {

        if (!CloverUtils.scrubCoverageData(initString, false)) {
            return false;
        }

        // now we need to trigger a reload.
        reload(true);

        return true;
    }

    @Override
    public void setSourcePath(Path path) {
        sourcePath = path;
    }

    @Override
    public float getCurrentCoverage() {
        final FullProjectInfo projectInfo = getSelectedScopeModel();
        return projectInfo == null ? -1 : projectInfo.getMetrics().getPcCoveredElements();
    }

    /**
     * Returns a Full, AppOnly or TestOnly model depending on selected scope.
     *
     * @return FullProjectInfo for selected scope
     * @see org.openclover.idea.config.IdeaCloverConfig#getModelScope()
     */
    @Nullable
    public FullProjectInfo getSelectedScopeModel() {
        final ModelScope scope = cloverConfig.getModelScope();
        final CloverDatabase model = getCoverage();
        return ModelUtil.getModel(model, scope);
    }


    private class CancellableCoverageLoadTaskDelegate extends AbstractCancellableTaskDelegate {
        private CoverageTreeModel newModel;
        private CoverageTreeModel origModel;
        private final boolean forcePerTestData;

        public CancellableCoverageLoadTaskDelegate(boolean forcePerTestData) {
            super("Loading Coverage data");
            this.forcePerTestData = forcePerTestData;
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            // caution - may be re-executed
            origModel = getCoverageTree();
            newModel = null;
            if (origModel != null) {
                newModel = origModel.safeCopy(progressIndicator);
                final ProgressIndicatorAdapter progressListener = new ProgressIndicatorAdapter(progressIndicator);
                newModel.loadCoverageData(forcePerTestData || loadPerTestData, progressListener);
                progressListener.handleProgress("Applying 'Passed Tests Coverage Only' filter", 0);
                newModel.applyIncludePassedTestCoverageOnlyFilter(cloverConfig.isIncludePassedTestCoverageOnly());
                progressListener.handleProgress("Creating tree model", 0);
                newModel.createTree();
                progressListener.handleProgress("Tree model ready", 0);
            }
        }

        @Override
        public void onSuccess() {
            // see if the model that we reloaded is still the same one
            if (origModel == getCoverageTree()) {
                // nothing happened, just apply changes
                setCoverageTree(newModel);
            } else {
                // model has been reloaded in the meantime, discard
                Logger.getInstance().debug("Discarding loaded data as the model has been changed in the meantime");
            }
        }
    }

    private class CancellableReloadTaskDelegate extends AbstractCancellableTaskDelegate {
        private final CoverageTreeModel newModel;
        private final AcceptsCoverageTreeModel recipient;

        public CancellableReloadTaskDelegate(CoverageTreeModel newModel, AcceptsCoverageTreeModel recipient) {
            super("Loading Clover coverage database");
            this.newModel = newModel;
            this.recipient = recipient;
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            final ProgressIndicatorAdapter progressListener = new ProgressIndicatorAdapter(progressIndicator);
            newModel.load(progressListener);
            progressListener.handleProgress("Applying 'Passed Tests Coverage Only' filter", 0);
            newModel.applyIncludePassedTestCoverageOnlyFilter(cloverConfig.isIncludePassedTestCoverageOnly());
            progressListener.handleProgress("Creating tree model", 0);
            newModel.createTree();
            progressListener.handleProgress("Tree model ready", 0);
        }

        @Override
        public void onSuccess() {
            recipient.setCoverageTree(newModel);
        }
    }
}
