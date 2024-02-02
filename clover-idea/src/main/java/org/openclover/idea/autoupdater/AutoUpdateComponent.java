package com.atlassian.clover.idea.autoupdater;

import com.atlassian.clover.versions.LibraryVersion;
import com.atlassian.clover.Logger;
import com.atlassian.clover.idea.PluginVersionInfo;
import com.atlassian.clover.idea.util.l10n.CloverIdeaPluginMessages;
import com.atlassian.clover.idea.config.AutoUpdateConfigPanel;
import com.atlassian.clover.idea.util.ui.ExceptionDialog;
import com.atlassian.clover.idea.util.NetUtil;
import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.io.InputStream;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

@State(name = "CloverAutoUpdate", storages = {@Storage(id = "other", file = "$APP_CONFIG$/other.xml")})
public class AutoUpdateComponent implements ApplicationComponent, Runnable, PersistentStateComponent<AutoUpdateConfiguration> {
    private static final long INIT_DELAY = 60; //Wait until the Intellij is well and truly up as checking for updates can cause deadlocks
    private static final long INTERVAL = 24 * 60 * 60;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    public static final String STABLE_URL = "https://openclover.org/update/latestStableVersion.xml";
    private final Logger LOG = Logger.getInstance("autoupdate");

    private ScheduledFuture<?> scheduledFuture;
    private LatestVersionInfo lastVersion;
    private boolean newVersionAvailable;

    private AutoUpdateConfiguration config = new AutoUpdateConfiguration();

    @Override
    @NotNull
    public String getComponentName() {
        return "Clover Autoupdater";
    }

    @Override
    public void initComponent() {
        if (config.isAutoUpdate()) {
            startScheduler();
        }
    }

    @Override
    public void disposeComponent() {
        stopScheduler();
    }

    private void startScheduler() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (scheduledFuture == null) {
            scheduledFuture = JobScheduler.getScheduler().scheduleWithFixedDelay(this, INIT_DELAY, INTERVAL, TIME_UNIT);
        }
    }

    private void stopScheduler() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    @Override
    public synchronized AutoUpdateConfiguration getState() {
        return config;
    }

    //this may happen before initComponent if there is saved non-default config
    @Override
    public void loadState(AutoUpdateConfiguration state) {
        synchronized (this) {
            // for safe publish to the scheduled thread
            config = new AutoUpdateConfiguration(state);
        }
        if (config.isAutoUpdate()) {
            startScheduler();
        } else {
            stopScheduler();
        }
    }

     public SearchableConfigurable getConfigurable() {
        return configurable;
    }

     private final SearchableConfigurable configurable =  new SearchableConfigurable() {
        @Override
        public String getId() {
            return "Clover Auto Update";
        }

        @Override
        public Runnable enableSearch(String s) {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "Auto Update";
        }

        @Override
        public String getHelpTopic() {
            return null;
        }

        private AutoUpdateConfigPanel configPanel;

        @Override
        public JComponent createComponent() {
            if (configPanel == null) {
                configPanel = new AutoUpdateConfigPanel();
                configPanel.setAutoUpdateConfig(config);
            }
            return configPanel;
        }

        @Override
        public void disposeUIResources() {
            configPanel = null;
        }

        @Override
        public boolean isModified() {
            return !config.equals(configPanel.getAutoUpdateConfig());
        }

        @Override
        public void apply() throws ConfigurationException {
            loadState(configPanel.getAutoUpdateConfig());
        }

        @Override
        public void reset() {
            configPanel.setAutoUpdateConfig(config);
        }

    };


    private static String getValue(Document document, String path) {
        try {
            final Element element = (Element) XPath.selectSingleNode(document, path);
            if (element == null || element.getValue() == null) {
                Logger.getInstance("autoupdate").info("Version info descriptor does not contain " + path);
                return null;
            }
            return element.getValue().trim();
        } catch (JDOMException e) {
            Logger.getInstance("autoupdate").info("Error parsing version info descriptor");
            return null;
        }
    }

    static LatestVersionInfo parse(Document document) {
        return new LatestVersionInfo(
                getValue(document, "/response/version/number"),
                getValue(document, "/response/version/downloadUrl"),
                getValue(document, "/response/version/releaseNotes"),
                getValue(document, "/response/version/releaseNotesUrl")
        );
    }

    private void handleVersion(@NotNull final LatestVersionInfo versionInfo) {
        setNewVersion(versionInfo);
        updateNotifiers();
    }

    private static void updateNotifiers() {
        final Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            final NewVersionNotifier notifier = project.getComponent(NewVersionNotifier.class);
            if (notifier != null) {
                notifier.update();
            }
        }
    }

    public boolean showNewVersionAvailable() {
        return newVersionAvailable && !downloadInProgressOrDone;
    }

    public LatestVersionInfo getLastVersion() {
        return lastVersion;
    }

    private void setNewVersion(@NotNull LatestVersionInfo versionInfo) {
        lastVersion = versionInfo;
        newVersionAvailable =
            new LibraryVersion(PluginVersionInfo.RELEASE_NUMBER).compareTo(new LibraryVersion(versionInfo.getNumber())) < 0
            && !getState().getIgnoredVersions().contains(versionInfo.getNumber());
    }

    /**
     * Stop showing new version icon until next poll.
     */
    public void resetLastVersion() {
        newVersionAvailable = false;
        updateNotifiers();
    }

    @Override
    public void run() {
        try {
            final LatestVersionInfo latestVersion = checkLatestVersion();
            if (latestVersion != null) {
                ApplicationManager.getApplication().invokeLater(() -> handleVersion(latestVersion));
            } else {
                LOG.info("Cannot retrieve latest version info (null)");
            }
        } catch (Exception e) {
            LOG.info("Cannot retrieve latest version info", e);
        }
    }

    /**
     * Retrieve information about the latest Clover version available for download
     *
     * @return LatestVersionInfo or <code>null</code> (reflection failure?)
     * @throws JDOMException in case of XML parsing error
     * @throws IOException in case of networking error
     */
    @Nullable
    public static LatestVersionInfo checkLatestVersion() throws JDOMException, IOException {
        final InputStream stream = NetUtil.openUrlStream(STABLE_URL);
        if (stream != null) {
            try {
                final Document document = new SAXBuilder().build(stream);
                return parse(document);
            } finally {
                stream.close();
            }
        }

        return null;
    }

    private boolean downloadInProgressOrDone;

    /*
        This one is meant to be called from dispatch thread - so the primitive-looking synchronization is sufficient. 
     */
    public void performUpdate(@NotNull String downloadUrl) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (downloadInProgressOrDone) {
            return;
        }
        downloadInProgressOrDone = true;
        updateNotifiers();
        final Task downloadTask = new CloverPluginDownloader(downloadUrl) {

            @Override
            public void onFailure(Exception ex) {
                downloadInProgressOrDone = false;
                updateNotifiers();
                ExceptionDialog.showOKDialog(null, CloverIdeaPluginMessages.getString("autoupdate.errordownloading"), "", ex, CloverIdeaPluginMessages.getString("autoupdate.errordownloading"));
            }

            // the Task.onSuccess is invokedLater(NON_MODAL), so it will not appear until you close the config window
            @Override
            public void onTrueSuccess() {
                // notice the downloadInProgressOrDone is not reset to false - version checker is disabled until after reboot
                final int resp = Messages.showYesNoDialog(CloverIdeaPluginMessages.getString("autoupdate.restart"), CloverIdeaPluginMessages.getString("autoupdate.updatingplugin"), Messages.getQuestionIcon());
                if (resp == DialogWrapper.OK_EXIT_CODE) {
                    ApplicationManager.getApplication().restart();
                }
            }

            @SuppressWarnings({"ResultOfMethodCallIgnored"})
            @Override
            public void onCancel() {
                downloadInProgressOrDone = false;
                updateNotifiers();
            }
        };

        downloadTask.queue();

    }


    public static AutoUpdateComponent getInstance() {
        return ApplicationManager.getApplication().getComponent(AutoUpdateComponent.class);
    }

    public void addIgnoredVersion(String number) {
        getState().addIgnoredVersion(number);
    }
}
