package com.atlassian.clover.eclipse.core.exclusion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.settings.ProjectSettings;
import com.atlassian.clover.eclipse.core.projects.settings.source.SourceFolderPattern;
import com.atlassian.clover.util.FilterUtils;

public class ExclusionFilter {
    protected final String[] includeFilter;
    protected final String[] excludeFilter;

    public ExclusionFilter(ProjectSettings settings) {
        final boolean instrumentSelected = settings.isInstrumentSelectedSourceFolders();
        if (instrumentSelected) {
            final List<SourceFolderPattern> folderPatterns = settings.getInstrumentedFolderPatterns();
            this.includeFilter = calculateIncludeFilter(folderPatterns);
            this.excludeFilter = calculateExcludeFilter(folderPatterns);
        } else {
            this.includeFilter = settings.calculateIncludeFilter();
            this.excludeFilter = settings.calculateExcludeFilter();
        }
    }

    public boolean isFilteredOut(IFile file) {
        //Get path of file, relative to project. By getting it as project relative
        //(hence not beginning with /), nodes that start with / are not similarly required
        String path = file.getProjectRelativePath().toPortableString();

        return !FilterUtils.isIncluded(path, excludeFilter, includeFilter, true)
                || (CloverPlugin.getInstance().isInWorkingSetMode() && !CloverPlugin.getInstance().getCloverWorkingSet()
                        .includes(file));
    }

    public String[] getIncludeFilter() {
        return includeFilter;
    }

    public String[] getExcludeFilter() {
        return excludeFilter;
    }

    static String[] calculateIncludeFilter(List<SourceFolderPattern> folderPatterns) {
        List<String> list = new ArrayList<>(folderPatterns.size());
        for (SourceFolderPattern pattern : folderPatterns) {
            if (pattern.isEnabled()) {
                list.addAll(unroll(pattern.getSrcPath(), pattern.getIncludePattern()));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    static String[] calculateExcludeFilter(List<SourceFolderPattern> folderPatterns) {
        List<String> list = new ArrayList<>(folderPatterns.size());
        for (SourceFolderPattern pattern : folderPatterns) {
            if (pattern.isEnabled()) {
                list.addAll(unroll(pattern.getSrcPath(), pattern.getExcludePattern()));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    static List<String> unroll(final String folder, String expression) {
        final String[] patterns = expression.split(",");
        final List<String> list = new ArrayList<>(patterns.length);
        for (String pattern : patterns) {
            pattern = pattern.trim();
            if (pattern.length() == 0) {
                continue;
            }
            final StringBuilder sb = new StringBuilder(folder.length() + 1 + pattern.length());
            sb.append(folder);
            if (pattern.charAt(0) != '/') {
                sb.append('/');
            }
            sb.append(pattern);
            list.add(sb.toString());
        }
        return list;
    }

}
