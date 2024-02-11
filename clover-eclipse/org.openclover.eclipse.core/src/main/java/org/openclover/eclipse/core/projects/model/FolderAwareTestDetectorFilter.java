package org.openclover.eclipse.core.projects.model;

import org.openclover.core.instr.tests.TestDetector;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.core.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderAwareTestDetectorFilter implements TestDetector {
    private final List<File> srcFolders;

    public FolderAwareTestDetectorFilter(CloverProject project, List<String> selectedFolders) {
        srcFolders = new ArrayList<>(selectedFolders.size());
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
