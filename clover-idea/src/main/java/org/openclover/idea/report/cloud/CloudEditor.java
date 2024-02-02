package org.openclover.idea.report.cloud;

import com.atlassian.clover.api.registry.HasMetrics;
import org.openclover.idea.coverage.BaseCoverageNodeViewer;
import org.openclover.idea.coverage.CoverageNodeViewer;
import org.openclover.idea.util.vfs.DummyFileEditor;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.panels.VerticalBox;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;


public class CloudEditor extends DummyFileEditor implements CloudReportView, DataProvider {
    private final JEditorPane riskEditorPane;
    private final JEditorPane winsEditorPane;
    private final JLabel subjectLabel = new JLabel();
    private final CoverageNodeViewer nodeViewer;

    private final JComponent rootComponent;

    private final CloudEditorController controller;

    public CloudEditor(Project project, CloudVirtualFile virtualFile) {
        controller = new CloudEditorController(project, virtualFile, this);

        riskEditorPane = createEditorPane(project);
        winsEditorPane = createEditorPane(project);

        final JTabbedPane tabs = new JTabbedPane();
        tabs.add("Risks", new JScrollPane(riskEditorPane));
        tabs.add("Quick Wins", new JScrollPane(winsEditorPane));

        final JComponent topComponent = new VerticalBox();
        topComponent.add(createCtrlPane());
        nodeViewer = new CoverageNodeViewer();
        nodeViewer.setTestMethodsVisible(false);
        topComponent.add(nodeViewer.getPane());

        rootComponent = new DataAwarePanel(new BorderLayout());
        rootComponent.add(topComponent, BorderLayout.NORTH);
        rootComponent.add(tabs, BorderLayout.CENTER);

        controller.update(virtualFile.getCoverageManager().getCoverage());
    }

    private class DataAwarePanel extends JComponent implements DataProvider {
        public DataAwarePanel(BorderLayout layout) {
            super();
            setLayout(layout);
        }

        @Override
        @Nullable
        public Object getData(@NonNls String dataId) {
            return controller.getData(dataId);
        }
    }

    private static JEditorPane createEditorPane(Project project) {
        final JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.addHyperlinkListener(new ClassHyperlinkListener(project));

        return editorPane;
    }

    private JPanel createCtrlPane() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Cloud report for:"));

        subjectLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 5, 3));
        panel.add(subjectLabel, BorderLayout.CENTER);
        panel.add(createActionPanel(), BorderLayout.EAST);

        return panel;
    }

    private JPanel createActionPanel() {
        final JPanel panel = new JPanel();

        final ActionManager actionManager = ActionManager.getInstance();
        final ActionGroup cloudReportProjectGroup =
                (ActionGroup) actionManager.getAction("CloverPlugin.CloudReportProjectBar");
        final ActionToolbar cloudReportProjectToolbar =
                actionManager.createActionToolbar("CloudReportProjectToolbar", cloudReportProjectGroup, true);

        final ActionGroup cloudReportPackageGroup =
                (ActionGroup) actionManager.getAction("CloverPlugin.CloudReportPackageBar");
        final ActionToolbar cloudReportPackageToolbar =
                actionManager.createActionToolbar("CloudReportPackageToolbar", cloudReportPackageGroup, true);

        panel.add(cloudReportProjectToolbar.getComponent());
        panel.add(cloudReportPackageToolbar.getComponent());

        return panel;
    }


    @Override
    @NotNull
    public JComponent getComponent() {
        return rootComponent;
    }

    @Override
    @NotNull
    public String getName() {
        return "CloverCloudEditor";
    }


    @Override
    public void setRisksHtml(String risks) {
        riskEditorPane.setText(risks);
    }

    @Override
    public void setWinsHtml(String wins) {
        winsEditorPane.setText(wins);
    }

    @Override
    public void setSummaryIcon(Icon icon) {
        subjectLabel.setIcon(icon);
    }

    @Override
    public void setSummaryText(String summary) {
        subjectLabel.setText(summary);
    }

    @Override
    public void clean() {
        riskEditorPane.setText("");
        winsEditorPane.setText("");
        nodeViewer.clearNode();
        subjectLabel.setIcon(null);
        subjectLabel.setText("");
    }

    @Override
    public void setSummaryNode(HasMetrics element, BaseCoverageNodeViewer.TestPassInfo testPassInfo) {
        nodeViewer.setNode(element, testPassInfo);
    }


    @Override
    public void dispose() {
        controller.dispose();
    }

    @Override
    @Nullable
    public Object getData(@NonNls String dataId) {
        return controller.getData(dataId);
    }
}

