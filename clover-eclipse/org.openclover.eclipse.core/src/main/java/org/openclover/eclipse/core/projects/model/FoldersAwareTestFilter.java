package org.openclover.eclipse.core.projects.model;

import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import com.atlassian.clover.registry.BaseInvertableFilter;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.reporters.filters.AntPatternTestFilter;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import com.atlassian.clover.reporters.filters.EmptyTestFilter;
import com.atlassian.clover.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FoldersAwareTestFilter extends BaseInvertableFilter {
    private final List<File> srcFolders;

    public FoldersAwareTestFilter(boolean inverted, List<File> srcFolders) {
        super(inverted);
        this.srcFolders = srcFolders;
    }

    public FoldersAwareTestFilter(CloverProject project, List<String> selectedFolders) {
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
    public FoldersAwareTestFilter invert() {
        return new FoldersAwareTestFilter(!isInverted(), srcFolders);
    }

    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof FullClassInfo) {
            final FullClassInfo ci = (FullClassInfo) hm;
            final File file = ((FullFileInfo)ci.getContainingFile()).getPhysicalFile();
            boolean matched = false;
            for (File src : srcFolders) {
                if (FileUtils.isAncestorOf(src, file)) {
                    matched = true;
                    break;
                }
            }
            return isInverted() ^ matched; 
        }

        return true;
    }

    public static HasMetricsFilter.Invertable getFor(CloverProject project) {
        if (project != null) {
            final ProjectSettings settings = project.getSettings();
            if (settings != null) {
                switch (settings.getTestSourceFolders()) {
                    case ProjectSettings.Values.SELECTED_FOLDERS:
                        return new FoldersAwareTestFilter(project, settings.getSelectedTestFolders());
                    case ProjectSettings.Values.ALL_FOLDERS:
                        return new AntPatternTestFilter(project.getProject().getLocation().toString(),
                                                        settings.calculateTestIncludeFilter(),
                                                        settings.calculateTestExcludeFilter());
                    default:
                        return new EmptyTestFilter();
                }
            }
        }

        return new DefaultTestFilter();
    }
}

