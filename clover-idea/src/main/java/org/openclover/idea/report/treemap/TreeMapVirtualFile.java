package com.atlassian.clover.idea.report.treemap;

import com.atlassian.clover.idea.coverage.CoverageManager;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.util.vfs.DummyVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

public class TreeMapVirtualFile extends DummyVirtualFile {
    private static final Key<TreeMapVirtualFile> FILE_KEY = Key.create(TreeMapVirtualFile.class.getName());

    private final CoverageManager coverageManager;

    private TreeMapVirtualFile(Project project) {
        super("TreeMap Report");
        coverageManager = ProjectPlugin.getPlugin(project).getCoverageManager();
    }

    public CoverageManager getCoverageManager() {
        return coverageManager;
    }

    /**
     * Return an already open instance of TreeMapVirtualFile that is associated with the project or create new one.
     *
     * @param project current project
     * @return existing or new instance of TreeMapVirtualFile
     */
    public static TreeMapVirtualFile getInstance(Project project) {
        TreeMapVirtualFile vf = project.getUserData(FILE_KEY);
        if (vf == null) {
            vf = new TreeMapVirtualFile(project);
            project.putUserData(FILE_KEY, vf);
        }

        return vf;
    }
}
