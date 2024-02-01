package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.Logger;
import com.atlassian.clover.ant.AntFileSetUtils;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.util.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.openclover.util.Maps.newHashMap;
import static org.openclover.util.Sets.newHashSet;

public class FilesetFilter implements HasMetricsFilter {
    private final Set<File> files = newHashSet();
    private final Map<String, Set<String>> fileNamesToPaths = newHashMap();

    public FilesetFilter(Project p, List<FileSet> fileSets) {
        for (final FileSet fileset : fileSets) {
            final File baseDir = fileset.getDir(p);
            Logger.getInstance().verbose("Scanning files to filter in " + baseDir + " ; exists = " + baseDir.exists() + " ; is directory = " + baseDir.isDirectory());

            // warn if inclusion/exclusion patterns contain leading/trailing whitespace
            AntFileSetUtils.checkForNonTrimmedPatterns(fileset, p);

            final String[] includedFiles = fileset.getDirectoryScanner(p).getIncludedFiles();
            for (String includedFile : includedFiles) {
                final File file = new File(baseDir, includedFile);
                Logger.getInstance().verbose("Adding to fileset filter: " + file.getPath());

                Set<String> paths = fileNamesToPaths.computeIfAbsent(file.getName(), k -> newHashSet());
                paths.add(file.getAbsolutePath());

                files.add(file);
            }
        }
    }

    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof FullFileInfo) {
            final FullFileInfo fileInfo = (FullFileInfo)hm;
            if (exactMatch(fileInfo) || fuzzyMatch(fileInfo)) {
                Logger.getInstance().verbose("Including file " + fileInfo.getPhysicalFile().getPath());
                return true;
            } else {
                Logger.getInstance().verbose("Excluding file " + fileInfo.getPhysicalFile().getPath());
                return false;
            }
        }
        return true;  // only filter at the file level
    }

    private boolean exactMatch(FullFileInfo fileInfo) {
        boolean matches = files.contains(fileInfo.getPhysicalFile());
        Logger.getInstance().debug("Exact filter matching on " + fileInfo.getPhysicalFile().getPath() + ": " + matches);
        return matches;
    }

    private boolean fuzzyMatch(FullFileInfo fileInfo) {
        Logger.getInstance().debug("Fuzzy: trying to matching " + fileInfo.getPhysicalFile().getPath() + " with fuzzy matching");
        final Set<String> paths = fileNamesToPaths.get(fileInfo.getName());
        if (paths != null) {
            Logger.getInstance().debug("Fuzzy: paths for " + fileInfo.getName() + ": " + paths);
            final String packagePath = fileInfo.getPackagePath();
            for (String path : paths) {
                String normalizedPath = FileUtils.getNormalizedPath(path);
                if (normalizedPath.endsWith(packagePath)) {
                    Logger.getInstance().debug("Fuzzy: normalized path " + normalizedPath + " ends with packagePath " + packagePath);
                    return true;
                } else {
                    Logger.getInstance().debug("Fuzzy: normalized path " + normalizedPath + " does not end with packagePath " + packagePath);
                }
            }
            Logger.getInstance().debug("Fuzzy: no matching paths");
        } else {
            Logger.getInstance().debug("Fuzzy: no paths to match on for " + fileInfo.getName());
        }
        return false;
    }
}
