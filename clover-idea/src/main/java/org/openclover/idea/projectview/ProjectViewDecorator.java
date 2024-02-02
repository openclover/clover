package org.openclover.idea.projectview;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.projectView.impl.nodes.PackageElementNode;
import com.intellij.ide.projectView.impl.nodes.PackageViewModuleNode;
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.components.ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class ProjectViewDecorator implements TreeStructureProvider, ProjectComponent {
    @Override
    public Collection<AbstractTreeNode> modify(AbstractTreeNode parent, Collection<AbstractTreeNode> children, ViewSettings settings) {
        final Collection<AbstractTreeNode> newChildren = new ArrayList<>(children.size());
        boolean modified = false;
        for (AbstractTreeNode child : children) {
            if (PackageElementNodeWrapper.canAnnotate(child)) {
                newChildren.add(new PackageElementNodeWrapper((PackageElementNode) child));
                modified = true;
            } else if (PsiDirectoryNodeWrapper.canAnnotate(child)) {
                newChildren.add(new PsiDirectoryNodeWrapper((PsiDirectoryNode) child));
                modified = true;
            } else if (ClassTreeNodeWrapper.canAnnotate(child)) {
                newChildren.add(new ClassTreeNodeWrapper((ClassTreeNode) child));
                modified = true;
            } else if (ProjectViewModuleNodeWrapper.canAnnotate(child)) {
                newChildren.add(new ProjectViewModuleNodeWrapper((ProjectViewModuleNode) child));
                modified = true;
            } else if (PackageViewModuleNodeWrapper.canAnnotate(child)) {
                newChildren.add(new PackageViewModuleNodeWrapper((PackageViewModuleNode) child));
                modified = true;
            } else {
               newChildren.add(child);
            }

        }
        return modified ? newChildren : children;
    }

    @Override
    public Object getData(Collection<AbstractTreeNode> selected, String dataName) {
        return null;
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "Clover Project View Decorator";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }
}
