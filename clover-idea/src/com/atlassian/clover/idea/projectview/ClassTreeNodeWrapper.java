package com.atlassian.clover.idea.projectview;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.build.InclusionDetector;
import com.atlassian.clover.idea.build.ProjectInclusionDetector;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.feature.CloverFeatures;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;

class ClassTreeNodeWrapper extends ClassTreeNode {
    ClassTreeNodeWrapper(ClassTreeNode orig) {
        super(orig.getProject(), orig.getPsiClass(), orig.getSettings());
    }

    @Override
    public void update(PresentationData presentation) {
        super.update(presentation);
        if (getValue() == null) {
            return;
        }
        final PsiFile containingFile = getValue().getContainingFile();
        if (containingFile == null) {
            return;
        }
        final VirtualFile virtualFile = containingFile.getVirtualFile();
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(getProject());
        if (virtualFile != null && plugin != null && plugin.getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_ICON_DECORATION)) {
            final InclusionDetector detector = ProjectInclusionDetector.processFile(getProject(), virtualFile);
            PresentationUtil.overlayPresentation(presentation, detector.isIncluded());
        }
    }

    static boolean canAnnotate(AbstractTreeNode abstractNode) {
        if (!(abstractNode instanceof ClassTreeNode)) {
            return false;
        }
        final ClassTreeNode node = (ClassTreeNode) abstractNode;
        final PsiClass psiClass = node.getPsiClass();
        final PsiFile containingFile = psiClass.getContainingFile();
        if (containingFile == null) {
            return false;
        }
        final VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            return false;
        }
        return ModuleUtil.findModuleForPsiElement(psiClass) != null;

    }

}
