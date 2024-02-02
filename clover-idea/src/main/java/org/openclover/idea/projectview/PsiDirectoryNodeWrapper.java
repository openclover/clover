package org.openclover.idea.projectview;

import org.openclover.idea.IProjectPlugin;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiDirectory;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.feature.CloverFeatures;

public class PsiDirectoryNodeWrapper extends PsiDirectoryNode {
    public PsiDirectoryNodeWrapper(PsiDirectoryNode orig) {
        super(orig.getProject(), orig.getValue(), orig.getSettings());
    }

    @Override
    public void update(PresentationData presentation) {
        super.update(presentation);
        final PsiDirectory psiDirectory = getValue();
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(getProject());
        if (psiDirectory != null && plugin != null && plugin.getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_ICON_DECORATION)) {
            PresentationUtil.overlayPackagePresentation(presentation, psiDirectory);
        }
    }

    static boolean canAnnotate(AbstractTreeNode abstractNode) {
        if (!(abstractNode instanceof PsiDirectoryNode)) {
            return false;
        }
        final PsiDirectoryNode node = (PsiDirectoryNode) abstractNode;
        final PsiDirectory psiDirectory = node.getValue();
        return psiDirectory.getVirtualFile().isInLocalFileSystem() && ModuleUtil.findModuleForPsiElement(psiDirectory) != null;
    }

}
