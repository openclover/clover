package com.atlassian.clover.idea;

import com.atlassian.clover.CloverLicense;
import com.atlassian.clover.CloverLicenseDecoder;
import com.atlassian.clover.CloverLicenseInfo;
import com.atlassian.clover.CloverStartup;
import com.atlassian.clover.Logger;
import com.atlassian.clover.idea.config.CloverGlobalConfig;
import com.atlassian.clover.idea.config.IdeaXmlConfigConstants;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.config.LicenseConfigPanel;
import com.atlassian.clover.util.ClassPathUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.io.File;

@State(name = IdeaXmlConfigConstants.OTHER_XML_FILE_COMPONENT_NAME, storages = {@Storage(id = "other", file = "$APP_CONFIG$/other.xml")})
public class CloverPlugin implements ApplicationComponent, PersistentStateComponent<CloverGlobalConfig> {

    /**
     * A Logging adaptor that redirects all local logging to the configured
     * IDEA logger.
     */
    public class IdeaLogger extends Logger {

        private com.intellij.openapi.diagnostic.Logger ideaLog;

        public IdeaLogger(com.intellij.openapi.diagnostic.Logger ideaLog) {
            super();
            this.ideaLog = ideaLog;
        }

        @Override
        public void log(int level, String aMsg, Throwable t) {

            switch (level) {
                case Logger.LOG_VERBOSE:
                case Logger.LOG_DEBUG:
                    ideaLog.debug(aMsg);
                    if (t != null) {
                        ideaLog.debug(t);
                    }
                    break;

                case Logger.LOG_INFO:
                    ideaLog.info(aMsg);
                    if (t != null) {
                        ideaLog.info(t);
                    }
                    break;

                case Logger.LOG_WARN:
                    ideaLog.warn(aMsg, t);
                    break;

                case Logger.LOG_ERR:
                    if (t != null) {
                        ideaLog.error(aMsg, t);
                    } else {
                        ideaLog.error(aMsg);
                    }
                    break;

                default:
                    ideaLog.debug("<unknown log level> " + aMsg);
                    break;
            }
        }
    }

    /**
     * Default evaluation period
     */
    public static final long EVALUATION_PERIOD = 30L * CloverLicense.ONE_DAY;

    /**
     * Logger. Initialized in constructor as we change LoggerFactory into one which returns IdeaLogger
     */
    private final Logger LOG;

    /**
     * Global configuration
     */
    private CloverGlobalConfig globalConfig = new CloverGlobalConfig();

    /**
     * Decoded license key
     */
    private CloverLicense license;


    /**
     * Static accessor that provides a reference to this Component.
     *
     * @return the CloverPlugin application component.
     */
    public static CloverPlugin getPlugin() {
        return ApplicationManager.getApplication().getComponent(CloverPlugin.class);
    }

    public CloverPlugin() {
        Logger.setFactory(new Logger.Factory() {
            @Override
            public Logger getLoggerInstance(String category) {
                return new IdeaLogger(com.intellij.openapi.diagnostic.Logger.getInstance(category));
            }
        });

        LOG = Logger.getInstance(CloverPlugin.class.getName());
    }

    /**
     * @see ApplicationComponent#getComponentName()
     */
    @Override
    @NotNull
    public String getComponentName() {
        return "Clover";
    }

    /**
     * @see ApplicationComponent#initComponent()
     */
    @Override
    public void initComponent() {
        // run through initialisation sequence...
        CloverStartup.logVersionInfo(LOG);
        LOG.info("Plugin Version " + PluginVersionInfo.RELEASE_NUMBER +
                "[" + PluginVersionInfo.BUILD_NUMBER + "]");

        loadLicense();

        // TODO As we dropped IDEA 10.x we could add the <add-to-group group-id="CompilerErrorViewPopupMenu" anchor="last"/>
        // TODO for <action id="CloverPlugin.JumpToActualSource" ...> and remove the following lines.
        // TODO However, the LightIdeaTestCase does not register the CompilerErrorViewPopupMenu group which causes
        // TODO errors in our tests if we remove these lines and use the add-to-group.
        final ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup compilerPopupMenu = (DefaultActionGroup) actionManager.getAction(IdeActions.GROUP_COMPILER_ERROR_VIEW_POPUP);
        if (compilerPopupMenu == null) {
            compilerPopupMenu = new DefaultActionGroup();
            actionManager.registerAction(IdeActions.GROUP_COMPILER_ERROR_VIEW_POPUP, compilerPopupMenu);
        }

        // Note: CloverPlugin.JumpToActualSource is defined in plugin.xml
        final AnAction action = actionManager.getAction("CloverPlugin.JumpToActualSource");
        compilerPopupMenu.add(action, Constraints.LAST);
    }

    /**
     * @see ApplicationComponent#disposeComponent()
     */
    @Override
    public void disposeComponent() {
        // nothing to do
    }

    public long getInstallDate() {
        return globalConfig.getInstallDate();
    }

    @Nullable
    public CloverLicense getLicense() {
        return license;
    }

    protected void loadLicense() {
        final IdeaLicenseLoader ideaLicenseLoader = new IdeaLicenseLoader();
        license = ideaLicenseLoader.loadLicense(LOG);
        CloverStartup.setLicenseLoader(ideaLicenseLoader);
        // License check.
        CloverStartup.loadLicense(LOG, false, globalConfig.getInstallDate());
        boolean isActivated = false;
        if (!CloverLicenseInfo.TERMINATED) {

            isActivated = true;

        }
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            // trigger license check, enable Clover for all projects as a side-effect
            final IdeaCloverConfig cloverConfig = ProjectPlugin.getPlugin(project).getConfig();
            cloverConfig.setEnabled(isActivated);
            cloverConfig.notifyListeners();
        }
    }

    /**
     * Returns the earliest of: System#currentTimeMillis(), value set in <code>config</code> (if available)
     * and jar (or class dir) modification time stamp
     * @param config
     */
    protected long calculateInstallDate(CloverGlobalConfig config) {
        final long dateFromConfig = (config.isInstallDateSet() ? config.getInstallDate() : System.currentTimeMillis());
        final File jarFile = new File(ClassPathUtil.getCloverJarPath());
        return jarFile.exists() ? Math.min(dateFromConfig, jarFile.lastModified()) : dateFromConfig;
    }

    @Override
    public CloverGlobalConfig getState() {
        return globalConfig;
    }

    @Override
    public void loadState(CloverGlobalConfig state) {
        globalConfig = new CloverGlobalConfig(
                state.getLicenseText(),
                calculateInstallDate(state)); // recalculate installation date
    }

    /**
     *
     */
    class IdeaLicenseLoader implements CloverStartup.LicenseLoader {

        @Override
        @Nullable
        public CloverLicense loadLicense(Logger log) {
            try {
                final String licenseString =
                        (globalConfig.getLicenseText() != null && globalConfig.getLicenseText().trim().length() != 0)
                                ? globalConfig.getLicenseText() : PluginVersionInfo.EVAL_LICENSE;
                return CloverLicenseDecoder.decode(licenseString);

            } catch (Exception ex) {
                if (log != null) {
                    log.info("Error parsing license: " + ex.getMessage(), ex);
                }
                return null;
            }
        }
    }

    public SearchableConfigurable getConfigurable() {
        return configurable;
    }

    private final SearchableConfigurable configurable =  new SearchableConfigurable() {
        @Override
        @NotNull
        public String getId() {
            return "Clover License";
        }

        @Override
        public Runnable enableSearch(String s) {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "License";
        }

        @Override
        public String getHelpTopic() {
            return null;
        }

        private LicenseConfigPanel configPanel;

        @Override
        public JComponent createComponent() {
            if (configPanel == null) {
                configPanel = new LicenseConfigPanel();
                configPanel.setLicenseText(globalConfig.getLicenseText());
            }
            return configPanel;
        }

        @Override
        public void disposeUIResources() {
            configPanel = null;
        }

        @Override
        public boolean isModified() {
            return !configPanel.getLicenseText().equals(globalConfig.getLicenseText());
        }

        @Override
        public void apply() throws ConfigurationException {
            license = null;
            globalConfig  = new CloverGlobalConfig(
                    configPanel.getLicenseText(),  // copy license key from dialog
                    globalConfig.getInstallDate()
            );
            loadLicense();
        }

        @Override
        public void reset() {
            configPanel.setLicenseText(globalConfig.getLicenseText()); // restore license key in dialog
        }

    };
}
