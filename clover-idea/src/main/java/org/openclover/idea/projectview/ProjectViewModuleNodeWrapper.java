package org.openclover.idea.projectview;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.nodes.AbstractModuleNode;
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import org.openclover.idea.CloverModuleComponent;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.feature.CloverFeatures;

public class ProjectViewModuleNodeWrapper extends ProjectViewModuleNode {
    public ProjectViewModuleNodeWrapper(ProjectViewModuleNode orig) {
        super(orig.getProject(), orig.getValue(), orig.getSettings());
    }

    @Override
    public void update(PresentationData presentation) {
        super.update(presentation);
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(getProject());
        if (plugin != null && plugin.getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_ICON_DECORATION)) {
            final CloverModuleComponent component = CloverModuleComponent.getInstance(getValue());
            if (component != null) {
                final boolean excluded = component.getConfig().isExcluded();
                PresentationUtil.overlayPresentation(presentation, !excluded);
            }
        }
    }

    static boolean canAnnotate(AbstractTreeNode abstractNode) {
        if (!(abstractNode instanceof ProjectViewModuleNode)) {
            return false;
        }
        final AbstractModuleNode node = (AbstractModuleNode) abstractNode;
        final CloverModuleComponent component = CloverModuleComponent.getInstance(node.getValue());
        return component != null;
    }

}