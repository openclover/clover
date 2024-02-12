package org.openclover.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.openclover.eclipse.core.exclusion.ExclusionLabelDecorator;
import org.openclover.eclipse.core.projects.model.CoverageModelsMonitor;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;
import org.openclover.eclipse.core.settings.InstallationSettings;
import org.openclover.eclipse.core.settings.WorkspaceSettings;
import org.openclover.eclipse.core.ui.editors.java.EditorCoverageSynchronizer;
import org.openclover.eclipse.core.ui.workingset.CloverWorkingSet;
import org.openclover.eclipse.core.views.CustomColumnDefinition;
import org.openclover.eclipse.core.views.coverageexplorer.CoverageView;
import org.openclover.eclipse.core.views.dashboard.DashboardView;
import org.openclover.eclipse.core.views.testcontributions.TestContributionsView;
import org.openclover.eclipse.core.views.testrunexplorer.TestRunExplorerView;
import org.openclover.runtime.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Maps.newHashMap;

/**
 * The Clover plugin. Ta-dah!
 */
public class CloverPlugin extends AbstractUIPlugin {
    /* Wait 10s after plugin start() completes before starting background processes */
    private static final long STARTUP_BACKOFF_DELAY = 10000;

    public static final String ID = "org.openclover.eclipse.core";

    public static final IPath CLOVER_RUNTIME_VARIABLE = new Path("CLOVER_RUNTIME");


    public static CloverPlugin instance;
    private BundleListener startListener;

    public static CloverPlugin getInstance() {
        return instance;
    }

    private PluginLoggingAdapter cloverLogger;
    private CoverageModelsMonitor coverageMonitor;
    private EditorCoverageSynchronizer editorSynchronizer;
    private CloverWorkingSet workingSet;
    private List<CustomColumnDefinition> customColumns;
    private InstallationSettings installationSettings;
    private WorkspaceSettings workspaceSettings;

    public static boolean isLoggingDebugFor(String category) {
        return getInstance().isDebugging() && getInstance().debugPlatformOptionOn(category);
    }

    public static void logVerbose(String message) {
        getInstance().cloverLogger.verbose(message);
    }

    public static void logVerbose(String message, Throwable t) {
        getInstance().cloverLogger.verbose(message, t);
    }

    public static void logDebug(String message) {
        getInstance().cloverLogger.debug(message);
    }

    public static void logDebug(String message, Throwable t) {
        getInstance().cloverLogger.debug(message, t);
    }

    public static void logInfo(String message) {
        getInstance().cloverLogger.info(message);
    }

    public static void logInfo(String message, Throwable t) {
        getInstance().cloverLogger.info(message, t);
    }

    public static void logWarning(String message) {
        getInstance().cloverLogger.warn(message);
    }

    public static void logWarning(String message, Throwable t) {
        getInstance().cloverLogger.warn(message, t);
    }

    public static CoreException logAndThrowError(String message) {
        logError(message);
        return new CoreException(new Status(Status.ERROR, ID, 0, message, null));
    }

    public static CoreException logAndThrowError(String message, Throwable t) {
        logError(message, t);
        return new CoreException(new Status(Status.ERROR, ID, 0, message, t));
    }

    public static void logError(String message) {
        getInstance().cloverLogger.error(message);
    }

    public static void logError(String message, Throwable t) {
        getInstance().cloverLogger.error(message, t);
    }

    public CloverPlugin() {
        super();

        //Take over logging
        cloverLogger = new PluginLoggingAdapter(getLog(), false);
        Logger.setInstance(cloverLogger);


        instance = this;

        logInfo("Clover plugin constructed");
    }

    /**
     * This method is called upon plug-in activation
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        buildSettings();

        ensureInstallDateGenerated();

        setLoggingLevel(getInstallationSettings().getLoggingLevel());
        loadCustomColumns();
        buildWorkingSet();

        editorSynchronizer = new EditorCoverageSynchronizer(getWorkbench());
        coverageMonitor = new CoverageModelsMonitor();

        //Wait until the bundle is fully started before starting background jobs
        //as there is a real danger of classloader deadlock in Equinox when the bundle state changes
        //or so it appears. See CEP-313. This may be a case of shadow boxing. Unclear ATM.
        final long thisBundleId = context.getBundle().getBundleId();
        startListener = bundleEvent -> {
            if (bundleEvent.getBundle().getBundleId() == thisBundleId
                && bundleEvent.getType() == BundleEvent.STARTED) {
                new SystemJob("Clover background processing starter") {
                    @Override
                    protected IStatus run(IProgressMonitor progressMonitor) {
                        try {
                            startMonitorEditors();
                            startMonitoringCoverage();
                            listenForSettingsChanges();
                        } catch (Exception e) {
                            CloverPlugin.logError("Failed to start background processing", e);
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule(STARTUP_BACKOFF_DELAY);
            }
        };
        context.addBundleListener(startListener);
        logInfo("Clover plugin started");
    }

    private void startMonitoringCoverage() {
        coverageMonitor.start();
    }

    private void startMonitorEditors() {
        editorSynchronizer.syncWithCoverageSetting(installationSettings.getEditorCoverageStyle());
    }

    private void buildWorkingSet() {
        workingSet = new CloverWorkingSet(getWorkbench().getWorkingSetManager());
    }

    private void listenForSettingsChanges() {
        installationSettings.addListener(
                event -> {
                    final String propertyName = event.getKey();

                    if (InstallationSettings.Keys.LOGGING_LEVEL.equals(propertyName)) {
                        // event.getNewValue() returns null if current value is a default installation value
                        setLoggingLevel(event.getNewValue() != null ? (String)event.getNewValue() : getInstallationSettings().getLoggingLevel());
                    } else if (InstallationSettings.Keys.COVERAGE_STYLE_IN_EDITORS.equals(propertyName)) {
                        try {
                            editorSynchronizer.syncWithCoverageSetting(Integer.parseInt((String)event.getNewValue()));
                        } catch (ClassCastException e) {
                            logError("Coverage style setting invalid type: " + event.getNewValue().getClass().getName());
                        } catch (NumberFormatException e) {
                            logError("Coverage style setting invalid integer string: " + event.getNewValue());
                        }
                    } else if (InstallationSettings.Keys.SHOW_EXCLUSION_ANNOTATIONS.equals(propertyName)) {
                        ExclusionLabelDecorator.decorationChanged();
                    }
                }
        );
    }

    private void loadCustomColumns() {
        customColumns = installationSettings.getCustomColumns();
    }

    private void buildSettings() {
        installationSettings = new InstallationSettings();
        workspaceSettings = new WorkspaceSettings();
    }

    /**
     * This method is called when the plug-in is stopped
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            if (startListener != null) {
                context.removeBundleListener(startListener);
            }
            
            editorSynchronizer.dispose();
            coverageMonitor.stop();

            installationSettings.save();
            workspaceSettings.save();
        }
        finally {
            super.stop(context);
            cloverLogger.info("Clover plugin stopped");
        }
    }

    private String prefixed(String key) {
        return CloverPlugin.ID + ".preferences." + key;
    }

    private void ensureInstallDateGenerated() {
        long installDate = getInstallationSettings().getInstallDate();
        if (installDate == -1L) {
            installDate = System.currentTimeMillis();
            logInfo("Setting installation date for this product to " + installDate);
            getInstallationSettings().setInstallDate(installDate);
        }
    }

    public InstallationSettings getInstallationSettings() {
        return installationSettings;
    }

    public PluginLoggingAdapter getCloverLogger() {
        return cloverLogger;
    }

    private void setLoggingLevel(String propertyValue) {
        if (InstallationSettings.Values.NO_LOGGING_LEVEL.equals(propertyValue)) {
            cloverLogger.setEnabled(false);
        } else {
            boolean debug;
            boolean verbose;
            if (InstallationSettings.Values.DEBUG_LOGGING_LEVEL.equals(propertyValue)) {
                debug = true;
                verbose = true;
            } else
            if (InstallationSettings.Values.VERBOSE_LOGGING_LEVEL.equals(propertyValue)) {
                debug = false;
                verbose = true;
            } else {
                debug = false;
                verbose = false;
            }
            super.setDebugging(debug);
            Logger.setDebug(debug);
            Logger.setVerbose(verbose);
            cloverLogger.setEnabled(true);
            cloverLogger.info("Setting logging level: debug=" + debug + " verbose=" + verbose);
        }
    }

    public boolean debugPlatformOptionOn(String category) {
        return
            "true".equalsIgnoreCase(
                Platform.getDebugOption(
                    ID + (category == null ? "" : "/" + category)
                        + "/debug"));
    }

    public String getPluginOption(String option) {
        return Platform.getDebugOption(ID + "/" + option);
    }

    public List<ReportHistoryEntry> getReportHistory() {
        try {
            org.osgi.service.prefs.Preferences instancePrefs =
                Platform.getPreferencesService().getRootNode().node(InstanceScope.SCOPE).node(ID);

            int historyCount = instancePrefs.getInt(WorkspaceSettings.Keys.REPORT_HISTORY_COUNT, 0);
            LinkedList<ReportHistoryEntry> history = newLinkedList();
            for(int i = 0; i < historyCount; i++) {
                if (instancePrefs.nodeExists(WorkspaceSettings.Keys.REPORT_HISTORY_PREFIX + i)) {
                    history.add(
                        new ReportHistoryEntry(
                            instancePrefs.node(WorkspaceSettings.Keys.REPORT_HISTORY_PREFIX + i)));

                }
            }

            return history;
        } catch (BackingStoreException e) {
            return Collections.emptyList();
        }
    }

    private void setReportHistory(List<ReportHistoryEntry> history) {
        Map<String, ReportHistoryEntry> pathToHistory = newHashMap();

        //Keep only the last valid generated report for a given project
        for (ReportHistoryEntry report : history) {
            if (report.isValid()) {
                pathToHistory.put(report.getPath(), report);
            }
        }
        List<ReportHistoryEntry> savedHistory = newLinkedList(history);
        history.retainAll(pathToHistory.values());

        //Keep only the last N generated reports in any case
        savedHistory = savedHistory.subList(Math.max(0, savedHistory.size() - 5), savedHistory.size());

        org.osgi.service.prefs.Preferences instancePrefs =
            Platform.getPreferencesService().getRootNode().node(InstanceScope.SCOPE).node(ID);

        int nodeCount = 0;
        for (ReportHistoryEntry entry : savedHistory) {
            entry.saveTo(instancePrefs.node(WorkspaceSettings.Keys.REPORT_HISTORY_PREFIX + nodeCount++));
        }
        instancePrefs.putInt(WorkspaceSettings.Keys.REPORT_HISTORY_COUNT, nodeCount);
    }

    public void addReportToHistory(final ReportHistoryEntry report) {
        setReportHistory(
            new LinkedList<ReportHistoryEntry>(getReportHistory()) {
                {add(report);}
            });
    }


    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path.
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(ID, path);
    }

    public static Image getImage(String path) {
        ImageRegistry reg = getInstance().getImageRegistry();
        if (reg.getDescriptor(path) == null) {
            reg.put(path, ImageDescriptor.createFromURL(instance.getBundle().getEntry(path)));
        }
        return reg.get(path);
    }

    public CoverageModelsMonitor getCoverageMonitor() {
        return coverageMonitor;
    }

    public boolean isInWorkingSetMode() {
        final Preferences prefs = InstanceScope.INSTANCE.getNode(CloverPlugin.ID);
        return prefs.getBoolean(InstallationSettings.Keys.WORKING_SET_ENABLED, false);
    }

    public void setIsInWorkingSetMode(boolean enabled) throws BackingStoreException {
        final Preferences prefs = InstanceScope.INSTANCE.getNode(CloverPlugin.ID);
        prefs.putBoolean(InstallationSettings.Keys.WORKING_SET_ENABLED, enabled);
        prefs.flush();
        getCoverageMonitor().fireCoverageChange();
    }

    public CloverWorkingSet getCloverWorkingSet() {
        return workingSet;
    }

    public void showViews(IWorkbenchPage page) throws PartInitException {
        if (page.findView(CoverageView.ID) == null) {
            page.showView(CoverageView.ID);
        }
        if (page.findView(TestRunExplorerView.ID) == null) {
            page.showView(TestRunExplorerView.ID);
        }
        if (page.findView(TestContributionsView.ID) == null) {
            page.showView(TestContributionsView.ID);
        }
        if (page.findView(DashboardView.ID) == null) {
            page.showView(DashboardView.ID);
        }
    }

    public Version getJDTVersion() {
        try {
            Bundle jdtCore = Platform.getBundle("org.eclipse.jdt.core");
            String versionString = jdtCore == null ? null : (String)jdtCore.getHeaders().get("Bundle-Version");
            return versionString == null ? null : Version.parseVersion(versionString);
        } catch (Exception e) {
            CloverPlugin.logError("Unable to calculate JDT version", e);
            return null;
        }
    }
}
