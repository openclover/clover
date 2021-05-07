package org.openclover.eclipse.core.exclusion;

import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.eclipse.core.projects.settings.source.SourceFolderPattern;
import com.atlassian.clover.util.FilterUtils;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class ExclusionUtil {
    private ExclusionUtil() {
    }

    private static final String[] FLYWEIGHT = new String[0];

    public static boolean isPresent(ICloverExcludable excludable, boolean inExcludes) {
        try {
            final CloverProject cloverProject = CloverProject.getFor(excludable.getProject());
            return cloverProject != null
                    && isPresent(excludable, inExcludes, cloverProject.getSettings().isInstrumentSelectedSourceFolders());
        } catch (CoreException e) {
            return false;
        }
    }

    public static boolean isPresent(ICloverExcludable excludable, boolean inExcludes, boolean sourceRootBased) {

        final String pattern = createPattern(excludable, sourceRootBased);
        for (String existing : getExistingPatterns(excludable, inExcludes)) {
            if (pattern.equals(existing)) {
                return true;
            }
        }
        return false;
    }

    private static String createPattern(ICloverExcludable excludable, boolean sourceRootBased) {
        return (!sourceRootBased ? excludable.getPackageFragmentRoot().getResource().getProjectRelativePath().toString() + "/"
                : "")
                + (excludable.isLeaf() ? excludable.getPath() : (excludable.getPath() + "**"));
    }

    // returns true if full rebuild is required
    public static boolean togglePresence(ICloverExcludable excludable, boolean inExcludes, boolean sourceRootBased) {
        final String pattern = createPattern(excludable, sourceRootBased);
        final ProjectSettings settings = getCloverProjectSettings(excludable);
        if (settings == null) {
            return false;
        }

        if (settings.isInstrumentSelectedSourceFolders()) {
            final String srcPath = excludable.getPackageFragmentRoot().getResource().getProjectRelativePath().toString();
            final List<SourceFolderPattern> instrumentedFolderPatterns = settings.getInstrumentedFolderPatterns();
            final List<SourceFolderPattern> newFolderPatterns = new ArrayList<SourceFolderPattern>(instrumentedFolderPatterns
                    .size());
            for (SourceFolderPattern sfp : instrumentedFolderPatterns) {
                if (srcPath.equals(sfp.getSrcPath())) {
                    final String existingPatterns = inExcludes ? sfp.getExcludePattern() : sfp.getIncludePattern();
                    final String newPattern = toggleString(existingPatterns, pattern);
                    final SourceFolderPattern newSFP = new SourceFolderPattern(sfp.getSrcPath(), inExcludes ? sfp
                            .getIncludePattern() : newPattern, inExcludes ? newPattern : sfp.getExcludePattern(), sfp.isEnabled());
                    newFolderPatterns.add(newSFP);
                } else {
                    newFolderPatterns.add(sfp);
                }
            }
            settings.setInstrumentedFolderPatterns(newFolderPatterns);
        } else {
            final String existingPatterns = inExcludes ? settings.getExcludeFilter() : settings.getIncludeFilter();
            final String newPattern = existingPatterns != null ? toggleString(existingPatterns, pattern) : pattern;
            if (inExcludes) {
                settings.setExcludeFilter(newPattern);
            } else {
                settings.setIncludeFilter(newPattern);
                return existingPatterns == null || existingPatterns.length() == 0 || newPattern.length() == 0;
            }
        }
        
        return false;
    }

    static String toggleString(String origPatterns, String pattern) {
        final ArrayList<String> patterns = newArrayList(FilterUtils.tokenizePattern(origPatterns));
        if (patterns.remove(pattern)) {
            final StringBuilder sb = new StringBuilder(origPatterns.length());
            String separator = "";
            for (String s : patterns) {
                sb.append(separator);
                sb.append(s);
                separator = ", ";
            }
            return sb.toString();
        } else {
            return origPatterns.trim().length() == 0 ? pattern : (origPatterns + ", " + pattern);
        }

    }

    private static String[] getExistingPatterns(ICloverExcludable excludable, boolean inExcludes) {
        final ProjectSettings settings = getCloverProjectSettings(excludable);
        if (settings == null) {
            return FLYWEIGHT;
        }
        final String patterns;
        patternRetrieval: if (settings.isInstrumentSelectedSourceFolders()) {
            final String srcPath = excludable.getPackageFragmentRoot().getResource().getProjectRelativePath().toString();
            for (SourceFolderPattern sfp : settings.getInstrumentedFolderPatterns()) {
                if (srcPath.equals(sfp.getSrcPath())) {
                    patterns = inExcludes ? sfp.getExcludePattern() : sfp.getIncludePattern();
                    break patternRetrieval;
                }
            }
            return FLYWEIGHT;
        } else {
            patterns = inExcludes ? settings.getExcludeFilter() : settings.getIncludeFilter();
        }

        return patterns != null ? FilterUtils.tokenizePattern(patterns) : FLYWEIGHT;
    }

    private static ProjectSettings getCloverProjectSettings(ICloverExcludable excludable) {
        try {
            final CloverProject cloverProject = CloverProject.getFor(excludable.getProject());
            return cloverProject == null ? null : cloverProject.getSettings();
        } catch (CoreException e) {
            return null;
        }
    }

}
