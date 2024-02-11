package org.openclover.idea.config;

import org.openclover.idea.util.ui.CloverIcons;
import org.openclover.idea.IdeaIDEContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

public class ProjectConfigPanel extends JPanel implements ActionListener, ConfigChangeListener {

    private static final String LICENSED_CARD = "licensed card";

    private final List<ConfigPanel> configPanels = newArrayList();

    private EnabledConfigPanel enableControl;

    private JPanel compilationPane;
    private JPanel viewPane;
    private ContextFilterPane filterPane;
    private JTabbedPane tabbedPane;
    private JPanel licensedPane;

    private final Project project;
    private final IdeaCloverConfig ideaCloverConfig;

    public ProjectConfigPanel(Project proj, IdeaCloverConfig ideaCloverConfig) {
        project = proj;
        this.ideaCloverConfig = ideaCloverConfig;
        initComponents();
        initListeners();
    }

    private void initComponents() {
        CardLayout layout = new CardLayout();
        setLayout(layout);

        add(getLicensedPanel(), LICENSED_CARD);

        layout.show(this, LICENSED_CARD);

        enableConfig(true);
    }


    private JPanel getLicensedPanel() {
        if (licensedPane == null) {
            licensedPane = new JPanel(new GridBagLayout());

            licensedPane.add(getEnablePanel(), new GBC(1, 1).setFill(GBC.BOTH).setWeight(1.0, 0.0).setAnchor(GBC.NORTHWEST));
            licensedPane.add(getTabbedContent(), new GBC(1, 2).setFill(GBC.BOTH).setWeight(1.0, 1.0));
            licensedPane.add(new JPanel(), new GBC(1, 3).setFill(GBC.VERTICAL).setWeight(0.0, 0.001));
        }

        return licensedPane;
    }

    private void initListeners() {
        getEnablePanel().getEnabledInput().addActionListener(this);
        ideaCloverConfig.addConfigChangeListener(this);

        getTabbedContent().getModel().addChangeListener(changeEvent ->
                ideaCloverConfig.setLastProjectConfigTabSelected(getTabbedContent().getSelectedIndex()));
    }

    private EnabledConfigPanel getEnablePanel() {
        if (enableControl == null) {
            enableControl = new EnabledConfigPanel();
        }
        return enableControl;
    }

    /**
     * Use this through {@link IdeaCloverConfig#setLastProjectConfigTabSelected(int)} to trick tabbed pane to display
     * filter tab on next window open.
     */
    public static final int COMPILATION_TAB_INDEX = 0;
    public static final int FILTER_TAB_INDEX = 2;

    private JTabbedPane getTabbedContent() {
        if (tabbedPane == null) {
            tabbedPane = new JTabbedPane();

            tabbedPane.addTab(getCompilationPane().getName(), CloverIcons.COMPILE_ICON, getCompilationPane());
            tabbedPane.addTab(getViewPane().getName(), CloverIcons.VIEW_ICON, getViewPane());
            tabbedPane.addTab(getFilterPane().getName(), CloverIcons.FILTER_ICON, getFilterPane());

            int selectionHint = ideaCloverConfig.getLastProjectConfigTabSelected();
            if (selectionHint < 0 || selectionHint >= tabbedPane.getTabCount()) {
                Logger.getInstance(getClass().getName()).info("Invalid tab selection in the config ["
                        + selectionHint + "], resetting.");
                selectionHint = 0;
                ideaCloverConfig.setLastProjectConfigTabSelected(selectionHint);
            }
            tabbedPane.setSelectedIndex(selectionHint);
        }
        return tabbedPane;
    }

    private JPanel getCompilationPane() {

        if (compilationPane == null) {
            compilationPane = new JPanel();
            compilationPane.setName("Compilation");

            GridBagLayout gb = new GridBagLayout();
            compilationPane.setLayout(gb);

            IdeaIDEContext context = new IdeaIDEContext(project);
            InitStringConfigPanel initStrPanel = new InitStringConfigPanel(context);
            compilationPane.add(initStrPanel, new GBC(1, 1).setFill(GBC.BOTH).setWeight(1, 0));
            configPanels.add(initStrPanel);

            FlushPolicyConfigPanel flushPanel = new FlushPolicyConfigPanel();
            compilationPane.add(flushPanel, new GBC(1, 2).setFill(GBC.BOTH).setWeight(1, 0));
            configPanels.add(flushPanel);

            InstrumentationConfigPanel filterPanel = new InstrumentationConfigPanel();
            compilationPane.add(filterPanel, new GBC(2, 1).setFill(GBC.BOTH).setWeight(1.0, 0));
            configPanels.add(filterPanel);

            ProjectRebuildConfigPanel rebuildPanel = new ProjectRebuildConfigPanel();
            compilationPane.add(rebuildPanel, new GBC(2, 2).setFill(GBC.BOTH).setWeight(1.0, 0));
            configPanels.add(rebuildPanel);

            compilationPane.add(new JPanel(), new GBC(1, 3).setSpan(2, 1).setFill(GBC.BOTH).setWeight(0.0, 1.0));
        }
        return compilationPane;
    }

    private JPanel getViewPane() {
        if (viewPane == null) {
            viewPane = new JPanel();
            viewPane.setName("View");

            GridBagLayout gb = new GridBagLayout();
            viewPane.setLayout(gb);

            RefreshPolicyConfigPanel refreshPanel = new RefreshPolicyConfigPanel();
            viewPane.add(refreshPanel, new GBC(1, 1).setAnchor(GBC.NORTHWEST).setFill(GBC.BOTH).setWeight(1, 0));
            configPanels.add(refreshPanel);

            SpanConfigPanel spanConfigPanel = new SpanConfigPanel();
            viewPane.add(spanConfigPanel, new GBC(1, 2).setAnchor(GBC.NORTHWEST).setFill(GBC.BOTH).setWeight(1, 0));
            configPanels.add(spanConfigPanel);

            GeneralConfigPanel generalPanel = new GeneralConfigPanel();
            viewPane.add(generalPanel, new GBC(2, 1).setAnchor(GBC.NORTHWEST).setFill(GBC.BOTH).setWeight(1, 0.0));
            configPanels.add(generalPanel);

            PerTestLoadConfigPanel perTestDataPanel = new PerTestLoadConfigPanel();
            viewPane.add(perTestDataPanel, new GBC(2, 2).setAnchor(GBC.NORTHWEST).setFill(GBC.BOTH).setWeight(1, 0.0));
            configPanels.add(perTestDataPanel);

            SourceHighlightConfigPanel highlightPanel = new SourceHighlightConfigPanel();
            viewPane.add(highlightPanel, new GBC(1, 3).setSpan(2, 1).setFill(GBC.HORIZONTAL));
            configPanels.add(highlightPanel);

            viewPane.add(new JPanel(), new GBC(1, 4).setSpan(2, 1).setFill(GBC.BOTH).setWeight(0.0, 1.0));
        }
        return viewPane;
    }

    private JPanel getFilterPane() {
        if (filterPane == null) {
            filterPane = new ContextFilterPane(project);
            configPanels.add(filterPane);
        }
        return filterPane;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(650, 550);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public void commitTo(IdeaCloverConfig config) {
        for (ConfigPanel configPanel : configPanels) {
            configPanel.commitTo(config);
        }
        enableControl.commitTo(config);
        config.notifyListeners();
    }

    public void loadFrom(IdeaCloverConfig config) {
        for (ConfigPanel configPanel : configPanels) {
            configPanel.loadFrom(config);
        }
        enableControl.loadFrom(config);
        enableConfig(config.isEnabled());
    }

    public void enableConfig(boolean b) {
        for (ConfigPanel configPanel : configPanels) {
            configPanel.enableConfig(b);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        enableConfig(enableControl.getEnabledInput().isSelected());
    }


    public void cleanup() {
//        if (regexpPane != null) {
//            regexpPane.cleanup();
//        }
    }

    public static void main(String[] argv) {
        IdeaCloverConfig config = IdeaCloverConfig.fromProject(null);
        ProjectConfigPanel configPanel = new ProjectConfigPanel(null, null);
        configPanel.loadFrom(config);

        JFrame frame = new JFrame();
        frame.setSize(configPanel.getPreferredSize());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(configPanel, BorderLayout.CENTER);
        frame.validate();
        frame.setVisible(true);
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.ENABLED)) {
            ((CardLayout) getLayout()).show(this, LICENSED_CARD);
            loadFrom(ideaCloverConfig);
        }

    }
}
