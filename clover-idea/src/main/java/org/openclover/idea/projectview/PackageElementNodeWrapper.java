package org.openclover.idea.projectview;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.nodes.PackageElement;
import com.intellij.ide.projectView.impl.nodes.PackageElementNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.psi.PsiPackage;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.feature.CloverFeatures;

class PackageElementNodeWrapper extends PackageElementNode {
    PackageElementNodeWrapper(PackageElementNode orig) {
        super(orig.getProject(), orig.getValue(), orig.getSettings());
    }

    @Override
    protected void update(PresentationData presentation) {
        super.update(presentation);
        final PsiPackage pkg = getValue().getPackage();
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(getProject());
        if (plugin != null && plugin.getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_ICON_DECORATION)) {
            PresentationUtil.overlayPackagePresentation(presentation, pkg);
        }
    }

    
    static boolean canAnnotate(AbstractTreeNode abstractNode) {
        if (!(abstractNode instanceof PackageElementNode)) {
            return false;
        }
        PackageElementNode node = (PackageElementNode) abstractNode;
        final PackageElement element = node.getValue();
        return !element.isLibraryElement();
    }
}
