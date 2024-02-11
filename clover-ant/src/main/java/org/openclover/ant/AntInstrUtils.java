package org.openclover.ant;

import clover.org.apache.commons.lang3.StringUtils;
import org.openclover.ant.tasks.AntInstrumentationConfig;
import org.openclover.runtime.CloverNames;
import com.atlassian.clover.Contract;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.util.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AntInstrUtils {
    private static Logger LOG = Logger.getInstance();

    public static String getProp(@NotNull final Project project,
                                 String key, String defaultValue) {
        Contract.pre(key != null);
        String prop = project.getProperty(key);
        if (prop == null) {
            prop = defaultValue;
        }
        return prop;
    }

    @Nullable
    public static PatternSet calcInstrPatternSet(@NotNull final Project project) {
        final String includes = getProp(project, CloverNames.PROP_INCLUDES_PATTERNSET, "");
        final String excludes = getProp(project, CloverNames.PROP_EXCLUDES_PATTERNSET, "");

        PatternSet instrPatternSet = null;
        if (includes.length() > 0 || excludes.length() > 0) {
            instrPatternSet = new PatternSet();
            if (includes.length() > 0) {
                instrPatternSet.setIncludes(includes);
            } else {
                instrPatternSet.setIncludes("**");
            }
            if (excludes.length() > 0) {
                instrPatternSet.setExcludes(excludes);
            }
        }

        final AntInstrumentationConfig config = AntInstrumentationConfig.getFrom(project);
        if (config != null && config.getInstrPattern() != null) {
            if (instrPatternSet != null) {
                LOG.warn("Sub-element <files> of <clover-setup> overrides clover.includes/excludes properties.");
            }
            instrPatternSet = config.getInstrPattern();
        }

        return instrPatternSet;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static List<FileSet> calcInstrFileSets(@NotNull final Project project) {
        final AntInstrumentationConfig config = AntInstrumentationConfig.getFrom(project);
        return config != null ? config.getInstrFilesets() : null;
    }

    public static File createInstrDir(@Nullable final File basedir) throws CloverException {
        final File tmpDir;
        try {
            final File tempDir = FileUtils.getJavaTempDir();
            if (!tempDir.exists()) {
                throw new CloverException("The " + FileUtils.JAVA_IO_TMPDIR + " directory '" + tempDir + "' does not exist. Please ensure this is created.");
            } else if (!tempDir.canRead()) {
                throw new CloverException("The " + FileUtils.JAVA_IO_TMPDIR + " directory '" + tempDir + "' can not be read from. Please ensure it has appropriate permissions.");
            } else if (!tempDir.canWrite()) {
                throw new CloverException("The " + FileUtils.JAVA_IO_TMPDIR + " directory '" + tempDir + "' can not be written to. Please ensure it has appropriate permissions.");
            }
            if (basedir != null) {
                if (!basedir.isDirectory() && !basedir.mkdirs()) {
                    throw new CloverException("Failed to create temp directory '" + basedir + "'");
                }
                tmpDir = File.createTempFile("clover", null, basedir);
            } else {
                tmpDir = File.createTempFile("clover", null);
            }
            tmpDir.delete();

            if (!tmpDir.mkdirs() && !tmpDir.isDirectory()) {
                throw new CloverException("Failed to create temp directory: '" + basedir + "'");
            }
        } catch (IOException e) {
            throw new CloverException("Failed to create temp directory: '" + basedir + "'", e);
        }
        return tmpDir;
    }

    public static void cleanUpInstrDir(@Nullable final File tmpDir, boolean preserveInstr) {
        if (!preserveInstr && tmpDir != null) {
            FileUtils.deltree(tmpDir);
        }
    }

    public static void sieveSourceForInstrumentation(
            @NotNull final Project project,
            @Nullable final Path srcPath,
            @Nullable final PatternSet instrPatternSet,
            @Nullable final List<FileSet> instrFileSetsList,
            @NotNull final Collection<File> toCompile,
            @NotNull final Collection<File> toCopy,
            @NotNull final Collection<File> toInstrument) {
        if (instrPatternSet != null && srcPath != null) {
            // apply pattern sets if they are supplied
            LOG.verbose("<files/> element found in <clover-setup/> - filtering source found in" + Arrays.toString(srcPath.list()));
            sieveSourceByPatternSets(project, srcPath, instrPatternSet, toCompile, toCopy, toInstrument);
        } else if (instrFileSetsList != null) {
            // apply filesets if they are supplied
            LOG.verbose("<fileset/> element(s) found in <clover-setup/>");
            sieveSourceByDirSets(project, instrFileSetsList, toCompile, toCopy, toInstrument);
        } else {
            // no filtering, so clear the copy set and add every file to the instrumentation set.
            LOG.verbose("No <files/> nor <fileset/> elements found in <clover-setup/> - instrumenting all source found.");
            sieveSourceByAddingAll(toCompile, toCopy, toInstrument);

        }
    }

    public static void sieveSourceByPatternSets(
            @NotNull final Project project,
            @NotNull final Path srcPath,
            @NotNull final PatternSet instrPatternSet,
            @NotNull final Collection<File> toCompile,
            @NotNull final Collection<File> toCopy,
            @NotNull final Collection<File> toInstrument) {

        for (final String srcDir : srcPath.list()) {
            sieveSrcPath(project, instrPatternSet, toCompile, toCopy, toInstrument, srcDir);
        }
    }

    public static void sieveSourceByDirSets(
            @NotNull final Project project,
            @NotNull final List<FileSet> instrFileSetsList,
            @NotNull final Collection<File> toCompile,
            @NotNull final Collection<File> toCopy,
            @NotNull final Collection<File> toInstrument) {

        for (final FileSet fileSet : instrFileSetsList) {
            final DirectoryScanner ds = fileSet.getDirectoryScanner(project);
            final File baseDir = fileSet.getDir(project);
            LOG.verbose("Filtering source files found in " + baseDir.getAbsolutePath());

            // check whether patterns do not contain leading/trailing whitespaces and warn about it
            AntFileSetUtils.checkForNonTrimmedPatterns(fileSet, project);

            final String[] included = ds.getIncludedFiles();
            if (included.length > 0) {
                for (String anIncluded : included) {
                    final File inc = new File(baseDir, anIncluded);
                    LOG.verbose("Found file for instrumentation: " + inc.getAbsolutePath());
                    if (toCompile.contains(inc)) {
                        toInstrument.add(inc);
                        toCopy.remove(inc);
                    } else {
                        LOG.verbose("File " + inc.getAbsolutePath() + " not in original compile list - ignoring");
                    }
                    toCompile.remove(inc);
                }
            } else {
                LOG.verbose("No sources found");
            }

            final String[] excluded = ds.getExcludedFiles();
            if (excluded.length > 0) {
                for (String anExcluded : excluded) {
                    final File exc = new File(baseDir, anExcluded);
                    LOG.verbose("Found file to exclude from instrumentation: " + exc.getAbsolutePath());
                    if (toCompile.contains(exc)) {
                        toInstrument.remove(exc);
                        toCopy.add(exc);
                    } else {
                        LOG.verbose("File " + exc.getAbsolutePath() + " not in original compile list - ignoring");
                    }
                    toCompile.remove(exc);
                }
            } else {
                LOG.verbose("No sources found");
            }
        }

        if (toCompile.size() > 0) {
            LOG.verbose(toCompile.size() + " file(s) in the compile list were neither included or excluded for Clover instrumentation. Adding to the exclude list:");
            for (final File file : toCompile) {
                LOG.verbose(file.getAbsolutePath());
            }
            toCopy.addAll(toCompile);
        }
    }

    public static void sieveSourceByAddingAll(
            @NotNull final Collection<File> toCompile,
            @NotNull final Collection<File> toCopy,
            @NotNull final Collection<File> toInstrument) {

        toCopy.clear();
        toInstrument.addAll(toCompile);
    }

    public static void sieveSrcPath(
            @NotNull final Project project,
            @NotNull final PatternSet instrPatternSet,
            @NotNull final Collection<File> toCompile,
            @NotNull final Collection<File> toCopy,
            @NotNull final Collection<File> toInstrument,
            @NotNull final String srcDir) {

        final String[] includePatterns = instrPatternSet.getIncludePatterns(project);
        final String[] excludePatterns = instrPatternSet.getExcludePatterns(project);

        // check whether patterns do not contain leading/trailing whitespaces and warn about it
        AntFileSetUtils.checkForNonTrimmedPatterns(includePatterns, excludePatterns, srcDir);

        // note: not checking Logger.isVerbose() because it does not work with 'ant -d/ant -v'
        String message = "Filtering source files in: " + srcDir;
        if (includePatterns != null) {
            message += " include patterns: " + StringUtils.join(includePatterns, ",");
        }
        if (excludePatterns != null) {
            message += " exclude patterns: " + StringUtils.join(excludePatterns, ",");
        }
        LOG.verbose(message);

        final DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(srcDir);
        ds.setIncludes(includePatterns);
        ds.setExcludes(excludePatterns);
        ds.scan();
        for (String anIncluded : ds.getIncludedFiles()) {
            final File inc = new File(srcDir, anIncluded);
            if (toCompile.contains(inc)) {
                toInstrument.add(inc);
                toCopy.remove(inc);
            }
        }
        for (String anExcluded : ds.getExcludedFiles()) {
            final File exc = new File(srcDir, anExcluded);
            if (toCompile.contains(exc)) {
                toInstrument.remove(exc);
                toCopy.add(exc);
            }
        }

        // note: not checking Logger.isVerbose() because it does not work with 'ant -d/ant -v'
        for (File f : toInstrument) {
            LOG.verbose("Found file for instrumentation: " + f.getAbsolutePath());
        }
        for (File f : toCopy) {
            LOG.verbose("Excluding from instrumentation: " + f.getAbsolutePath());
        }
    }
}
