package org.openclover.idea;

import com.atlassian.clover.CloverStartup;
import org.openclover.runtime.Logger;
import org.openclover.idea.config.CloverGlobalConfig;
import org.openclover.idea.config.IdeaXmlConfigConstants;
import org.openclover.idea.config.IdeaCloverConfig;
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;


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
     * Logger. Initialized in constructor as we change LoggerFactory into one which returns IdeaLogger
     */
    private final Logger LOG;

    /**
     * Global configuration
     */
    private CloverGlobalConfig globalConfig = new CloverGlobalConfig();


    /**
     * Static accessor that provides a reference to this Component.
     *
     * @return the CloverPlugin application component.
     */
    public static CloverPlugin getPlugin() {
        return ApplicationManager.getApplication().getComponent(CloverPlugin.class);
    }

    public CloverPlugin() {
        Logger.setFactory(category -> new IdeaLogger(com.intellij.openapi.diagnostic.Logger.getInstance(category)));

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
        LOG.info("Plugin Version " + PluginVersionInfo.RELEASE_NUMBER);

        activateCloverForProjects();

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

    protected void activateCloverForProjects() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            final IdeaCloverConfig cloverConfig = ProjectPlugin.getPlugin(project).getConfig();
            cloverConfig.setEnabled(true);
            cloverConfig.notifyListeners();
        }
    }

    @Override
    public CloverGlobalConfig getState() {
        return globalConfig;
    }

    @Override
    public void loadState(CloverGlobalConfig state) {
        globalConfig = new CloverGlobalConfig();
    }

}
