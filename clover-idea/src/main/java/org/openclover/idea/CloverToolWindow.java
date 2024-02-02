package org.openclover.idea;

import org.openclover.idea.CloverToolWindowId;
import org.openclover.idea.util.ui.CloverIcons;
import com.atlassian.clover.registry.entities.FullClassInfo;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.util.ModelScope;
import org.openclover.idea.coverageview.CoverageViewPanel;
import org.openclover.idea.coverage.CoverageNodeViewer;
import org.openclover.idea.coverage.CoverageTreeModel;
import org.openclover.idea.report.jfc.WarningBox;
import org.openclover.idea.util.ui.BorderLayoutConverter;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.beans.PropertyChangeEvent;

public class CloverToolWindow extends JPanel implements ConfigChangeListener, NodeWrapperSelectionListener {

    private static final Logger LOG = Logger.getInstance(CloverToolWindow.class.getName());

    private ToolWindow toolWindow;

    private ActionToolbar toolbar;
    private CoverageViewPanel coveragePane;
    private JPanel coverageSummaryPanel;
    private CoverageNodeViewer coverageNodeViewer;
    private WarningBox warningBox;

    private final Project project;


    private static final Key<CloverToolWindow> WINDOW_PROJECT_KEY =
            Key.create(CloverToolWindow.class.getName());

    public static CloverToolWindow getInstance(Project project) {
        CloverToolWindow window = project.getUserData(WINDOW_PROJECT_KEY);
        if (window == null) {
            window = new CloverToolWindow(project);
            project.putUserData(WINDOW_PROJECT_KEY, window);
        }

        return window;
    }

    private CloverToolWindow(Project project) {
        this.project = project;

        initComponents();
        initListeners();
    }

    //---( Implementation of the ConfigChangeListener interface )---

    /**
     * @see org.openclover.idea.config.ConfigChangeListener#configChange(org.openclover.idea.config.ConfigChangeEvent)
     */
    @Override
    public void configChange(ConfigChangeEvent evt) {
        // Monitor config changes for a change in the show summary in toolwindow property.
        // When true, we want to show the coverage summar panel. When false, we hide it.
        if (evt.hasPropertyChange(IdeaCloverConfig.SHOW_SUMMARY_IN_TOOLWINDOW)) {
            PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.SHOW_SUMMARY_IN_TOOLWINDOW);
            boolean show = (Boolean) propertyChange.getNewValue();
            getCoverageSummaryPanel().setVisible(show);
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.ENABLED)) {
            PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.ENABLED);
            boolean enabled = (Boolean) propertyChange.getNewValue();
            if (toolWindow != null) {
                toolWindow.setAvailable(enabled, null);
            }
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.MODEL_SCOPE)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.MODEL_SCOPE);
            ModelScope scope = (ModelScope) propertyChange.getNewValue();
            updateCoverageNodeViewScope(scope);
        }
    }

    private void updateCoverageNodeViewScope(ModelScope scope) {
        getCoverageNodeViewer().setTestMethodsVisible(scope == ModelScope.ALL_CLASSES || scope == ModelScope.TEST_CLASSES_ONLY);
    }

    private void initComponents() {
        setLayout(new BorderLayoutConverter(this));
        setBorder(new EmptyBorder(2, 2, 2, 2));

        final Container c = new Container();
        c.setLayout(new BorderLayout());
        c.add(getToolBar(), BorderLayout.NORTH);
        c.add(getCoveragePane(), BorderLayout.CENTER);

        add(c, BorderLayout.CENTER);
        add(getCoverageSummaryPanel(), BorderLayout.SOUTH);

        getCoverageSummaryPanel().setVisible(getConfig().isShowSummaryInToolwindow());
    }

    private void initListeners() {
        final IdeaCloverConfig config = getConfig();
        config.addConfigChangeListener(this);
        updateCoverageNodeViewScope(config.getModelScope());

        getCoveragePane().addElementSelectionListener(this);

    }

    @Override
    public void elementSelected(CoverageTreeModel.NodeWrapper wrapper) {
        // misplaced logic again, should go directly to CoverageNodeViewer
        if (wrapper == null) {
            getCoverageNodeViewer().clearNode();
            getWarningBox().setVisible(false);
        } else {
            final HasMetrics hasMetrics = wrapper.getHasMetrics();
            getCoverageNodeViewer().setNode(hasMetrics, wrapper.getTestPassInfo());

            // determine whether the file represented by this node is out of date.
            try {
                FullFileInfo fileInfo = null;
                if (hasMetrics instanceof FullClassInfo) {
                    fileInfo = (FullFileInfo) ((FullClassInfo) hasMetrics).getContainingFile();
                } else if (hasMetrics instanceof FullFileInfo) {
                    fileInfo = (FullFileInfo) hasMetrics;
                }

                if (fileInfo != null && !fileInfo.validatePhysicalFile()) {
                    getWarningBox().setMessage("Coverage data for " + fileInfo.getPhysicalFile().getName() + " is out of date.");
                    getWarningBox().setVisible(true);
                    // ensure that the currently selected path is still visible.
                    SwingUtilities.invokeLater(() -> getCoveragePane().ensureSelectionVisible());
                } else {
                    getWarningBox().setVisible(false);
                }
            } catch (Exception e) {
                LOG.error(e);
                getWarningBox().setVisible(false);
            }
        }
    }


    private IdeaCloverConfig getConfig() {
        return ProjectPlugin.getPlugin(project).getConfig();
    }

    private JComponent getToolBar() {
        if (toolbar == null) {
            ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("CloverToolBar");
            toolbar = ActionManager.getInstance().createActionToolbar("Clover", actionGroup, true);
        }
        return toolbar.getComponent();
    }

    private CoverageViewPanel getCoveragePane() {
        if (coveragePane == null) {
            coveragePane = new CoverageViewPanel(project, CloverToolWindowId.TOOL_WINDOW_ID);
        }
        return coveragePane;
    }

    private JComponent getCoverageSummaryPanel() {
        if (coverageSummaryPanel == null) {
            coverageSummaryPanel = new JPanel(new BorderLayout());
            JPanel middlePopups = new JPanel(new BorderLayout());
            middlePopups.add(getWarningBox(), BorderLayout.CENTER);
            coverageSummaryPanel.add(middlePopups, BorderLayout.NORTH);
            coverageSummaryPanel.add(getCoverageNodeViewer().getPane(), BorderLayout.CENTER);
        }
        return coverageSummaryPanel;
    }

    private CoverageNodeViewer getCoverageNodeViewer() {
        if (coverageNodeViewer == null) {
            coverageNodeViewer = new CoverageNodeViewer();
        }
        return coverageNodeViewer;
    }

    private WarningBox getWarningBox() {
        if (warningBox == null) {
            warningBox = new WarningBox();
            warningBox.setVisible(false);
        }
        return warningBox;
    }

    public boolean isRegistered() {
        return toolWindow != null;
    }

    /**
     * Register the CloverToolWindow with the ToolWindowManager, making it
     * available to the client.
     */
    public void register() {

        if (isRegistered()) {
            return; // already registered.
        }

        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindow = toolWindowManager.registerToolWindow(CloverToolWindowId.TOOL_WINDOW_ID, false, ToolWindowAnchor.LEFT);

        final ContentManager contentManager = toolWindow.getContentManager();
        final Content content = contentManager.getFactory().createContent(this, null, false);
        contentManager.addContent(content);
        toolWindow.setIcon(CloverIcons.TOOL_WINDOW);
        toolWindow.setAvailable(getConfig().isEnabled(), null);
    }


    public void cleanup() {
        unregister();
    }

    /**
     * Unregister the CloverToolWindow from the ToolWindowManager. Once
     * unregistered, the ToolWindow will not be avalable to the client.
     */
    private void unregister() {
        if (!isRegistered()) {
            return; // not registered
        }
        final ToolWindowManager tManager = ToolWindowManager.getInstance(project);
        tManager.unregisterToolWindow(CloverToolWindowId.TOOL_WINDOW_ID);
        toolWindow = null;
    }
}
