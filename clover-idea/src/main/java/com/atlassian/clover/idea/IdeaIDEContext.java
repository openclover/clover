package com.atlassian.clover.idea;

import com.atlassian.clover.idea.util.ProjectUtil;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.intellij.openapi.project.Project;

import javax.swing.Icon;
import java.io.File;

public class IdeaIDEContext extends IDEContext {

    private final Project project;

    @Override
    public String getContextFilterSpec() {
        return ProjectPlugin.getPlugin(project).getConfig().getContextFilterSpec();
    }

    public IdeaIDEContext(Project project) {
        this.project = project;
    }

    @Override
    public File getProjectRootDirectory() {
        return ProjectUtil.getProjectDirectory(project);
    }

    @Override
    public String getProjectName() {
        return project.getName();
    }

    @Override
    public Icon[] getFileTypes() {
        return new Icon[]{CloverIcons.HTML_FILETYPE, CloverIcons.PDF_FILETYPE, CloverIcons.XML_FILETYPE};
    }

    @Override
    public File getCloverWorkspace() {
        return ProjectUtil.getProjectWorkspace(project);
    }
}
