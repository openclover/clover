package org.openclover.idea.projectview;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.CloverModuleComponent;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.feature.CloverFeatures;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.nodes.AbstractModuleNode;
import com.intellij.ide.projectView.impl.nodes.PackageViewModuleNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;

public class PackageViewModuleNodeWrapper extends PackageViewModuleNode {
    public PackageViewModuleNodeWrapper(PackageViewModuleNode orig) {
        super(orig.getProject(), orig.getValue(), orig.getSettings());
    }

    @Override
    public void update(PresentationData presentation) {
        super.update(presentation);
        final CloverModuleComponent component = CloverModuleComponent.getInstance(getValue());
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(getProject());
        if (component != null && plugin != null && plugin.getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_ICON_DECORATION)) {
            final boolean excluded = component.getConfig().isExcluded();
            PresentationUtil.overlayPresentation(presentation, !excluded);
        }
    }

    static boolean canAnnotate(AbstractTreeNode abstractNode) {
        if (!(abstractNode instanceof PackageViewModuleNode)) {
            return false;
        }
        final AbstractModuleNode node = (AbstractModuleNode) abstractNode;
        final CloverModuleComponent component = CloverModuleComponent.getInstance(node.getValue());
        return component != null;
    }

}