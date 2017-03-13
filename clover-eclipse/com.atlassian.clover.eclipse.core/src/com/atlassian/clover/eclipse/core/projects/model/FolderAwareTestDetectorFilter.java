package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderAwareTestDetectorFilter implements TestDetector {
    private final List<File> srcFolders;

    public FolderAwareTestDetectorFilter(CloverProject project, List<String> selectedFolders) {
        srcFolders = new ArrayList<File>(selectedFolders.size());
        final IProject iProject = project.getProject();
        for (String selectedFolder : selectedFolders) {
            final IResource srcFolder = iProject.findMember(selectedFolder);
            if (srcFolder != null) {
                srcFolders.add(srcFolder.getLocation().toFile());
            }
        }
    }


    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return isInTestFolder(sourceContext.getSourceFile());
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return isInTestFolder(sourceContext.getSourceFile());
    }

    private boolean isInTestFolder(File file) {
        for (File src : srcFolders) {
            if (FileUtils.isAncestorOf(src, file)) {
                return true;
            }
        }

        return false;
    }
}
