package com.atlassian.clover.idea.util.ui;

import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.ConfigChangeEvent;
import com.atlassian.clover.idea.config.ConfigChangeListener;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.testexplorer.CoverageDataHolder;
import com.atlassian.clover.idea.treetables.TreeTableModelFactory;
import com.atlassian.clover.registry.FileInfoRegion;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ScrollToSourceMouseAdapter extends MouseAdapter implements ConfigChangeListener {
    private final Project project;
    private int requiredClickCount;

    private ScrollToSourceMouseAdapter(Project project) {
        this.project = project;

        IdeaCloverConfig config = ProjectPlugin.getPlugin(project).getConfig();
        config.addConfigChangeListener(this);
        requiredClickCount = config.isAutoScroll() ? 1 : 2;
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.AUTO_SCROLL_TO_SOURCE)) {
            final boolean autoScroll = (Boolean) evt.getPropertyChange(IdeaCloverConfig.AUTO_SCROLL_TO_SOURCE).getNewValue();
            requiredClickCount = autoScroll ? 1 : 2;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() != requiredClickCount) {
            return;
        }

        final Component component = e.getComponent();
        final JTree theTree = (component instanceof JTree) ? (JTree) component : TreeTableModelFactory.retrieveJTree(component);
        if (theTree == null) {
            return;
        }

        final TreePath path = theTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        final FileInfoRegion region;
        final Object object = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (object instanceof FileInfoRegion) {
            region = (FileInfoRegion) object;
        } else if (object instanceof TestCaseInfo) {
            region = ((TestCaseInfo) object).getSourceMethod();
        } else if (object instanceof CoverageDataHolder) {
            final HasMetrics hasMetrics = ((CoverageDataHolder) object).getElement();
            if (hasMetrics instanceof FileInfoRegion) {
                region = (FileInfoRegion) hasMetrics;
            } else {
                throw new IllegalArgumentException("Unexpected CoverageDataHolder content: " + hasMetrics.getClass());
            }
        } else {
            return;
        }
        if (region != null) {
            ScrollUtil.scrollToSourceRegion(project, region);
        }
    }

    private static final Key<ScrollToSourceMouseAdapter> ADAPTER_PROJECT_KEY =
            Key.create(ScrollToSourceMouseAdapter.class.getName());

    public static ScrollToSourceMouseAdapter getInstance(Project project) {
        ScrollToSourceMouseAdapter instance = project.getUserData(ADAPTER_PROJECT_KEY);
        if (instance == null) {
            instance = new ScrollToSourceMouseAdapter(project);
            project.putUserData(ADAPTER_PROJECT_KEY, instance);
        }
        return instance;
    }

}
