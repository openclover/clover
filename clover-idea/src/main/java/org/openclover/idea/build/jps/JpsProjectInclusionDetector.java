package org.openclover.idea.build.jps;

import org.openclover.core.spi.lang.Language;
import org.openclover.core.util.FileUtils;
import org.openclover.idea.util.InclusionUtil;
import org.openclover.idea.build.InclusionDetector;
import org.openclover.idea.config.CloverModuleConfig;
import org.openclover.idea.config.CloverPluginConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;

import java.io.File;

/**
 * Inclusion detector which works on the JpsProject model.
 *
 * @see org.openclover.idea.build.ProjectInclusionDetector
 */
public class JpsProjectInclusionDetector implements InclusionDetector {

    // default values
    private boolean isIncluded = false;
    private boolean cloverDisabled = false;
    private boolean notJava = false;
    private boolean moduleNotFound = false;
    private boolean moduleExcluded = false;
    private boolean pathNotFound = false;
    private boolean patternExcluded = false;
    private boolean inTestSources = false;

    @Nullable
    private JpsModule enclosingModule;

    /**
     * Analyze given <code>file</code> and return an InclusionDetector instance
     *
     * @param project current project
     * @param file    file to be checked
     * @return InclusionDetector
     */
    @NotNull
    public static InclusionDetector processFile(final @NotNull JpsProject project, final @NotNull File file) {
        // fetch Clover configuration associated with a project
        final CloverPluginConfig config = JpsModelUtil.getCloverPluginConfig(project);
        if (config != null) {
            return processFile(project, file, config);
        } else {
            // configuration not found, return disabled
            final JpsProjectInclusionDetector detector = new JpsProjectInclusionDetector();
            detector.cloverDisabled = false;
            return detector;
        }
    }

    /**
     * Hidden constructor as access shall be made via static factory method only.
     */
    private JpsProjectInclusionDetector() {

    }

    // interface

    @Override
    public boolean isIncluded() {
        return isIncluded;
    }

    @Override
    public boolean isModuleExcluded() {
        return moduleExcluded;
    }

    @Override
    public boolean isPathNotFound() {
        return pathNotFound;
    }

    @Override
    public boolean isPatternExcluded() {
        return patternExcluded;
    }

    @Override
    public boolean isInNoninstrumentedTestSources() {
        return inTestSources;
    }

    @Override
    public boolean isCloverDisabled() {
        return cloverDisabled;
    }

    @Override
    public boolean isNotJava() {
        return notJava;
    }

    @Override
    public boolean isModuleNotFound() {
        return moduleNotFound;
    }

    /**
     * Helper method for unit testing only.
     *
     * @return JpsModule
     */
    @Nullable
    protected JpsModule getEnclosingModule() {
        return enclosingModule;
    }

    /**
     * Analyze given <code>file</code> and return an InclusionDetector instance
     *
     * @param project current project
     * @param file    file to be checked
     * @param config  clover configuration
     * @return InclusionDetector
     */
    @NotNull
    protected static InclusionDetector processFile(final @NotNull JpsProject project,
                                                   final @NotNull File file,
                                                   final @NotNull CloverPluginConfig config) {
        final JpsProjectInclusionDetector detector = new JpsProjectInclusionDetector();

        // check if instrumentation is enabled
        detector.cloverDisabled = !(config.isEnabled() && config.isBuildWithClover());

        // processing non-java source files
        detector.notJava = !hasJavaFileExtension(file);

        // find module in which source file is located; check if module is excluded from instrumentation
        detector.enclosingModule = JpsModelUtil.findModuleForFile(project, file);
        if (detector.enclosingModule == null) {
            // unknown module - not instrumenting
            detector.moduleNotFound = true;
            return detector;
        } else {
            JpsSimpleElement<CloverModuleConfig> element = detector.enclosingModule.getContainer().getChild(
                    CloverSerializerExtension.CloverModuleConfigurationRole.INSTANCE);
            if (element != null && element.getData().isExcluded()) {
                // module excluded from instrumentation - not instrumenting
                detector.moduleExcluded = true;
                return detector;
            }
        }

        // get relative path to the source root and check inclusion/exclusion patterns
        final String javaPath = getRootRelativeFileName(detector.enclosingModule, file);
        if (javaPath == null) {
            detector.pathNotFound = true;
            return detector;
        }
        if (!InclusionUtil.isIncluded(javaPath, InclusionUtil.toArray(config.getExcludes(), " ,"),
                InclusionUtil.toArray(config.getIncludes(), " ,"), true)) {
            detector.patternExcluded = true;
            return detector;
        }

        // omit test sources if we don't instrument them
        if (!config.isInstrumentTests()) {
            if (isInTestSource(detector.enclosingModule, file)) {
                detector.inTestSources = true;
                return detector;
            }
        }

        // finally :-) yes this file can be instrumented
        detector.isIncluded = true;
        return detector;
    }

    /**
     * Search source root under which given <code>file</code> resides and return a relative path to it.
     *
     * @param module module containing this file
     * @param file   file location
     * @return String relative path (OS-specific separators are used) or <code>null</code> if source root was not found
     */
    @Nullable
    protected static String getRootRelativeFileName(final @NotNull JpsModule module,
                                                    final @NotNull File file) {
        final JpsModuleSourceRoot sourceRoot = JpsModelUtil.findSourceRootForFile(module, file);
        return sourceRoot != null ? FileUtils.getRelativePath(sourceRoot.getFile(), file) : null;
    }

    /**
     * Search source roots under which given <code>file</code> resides and return <code>true</code> if it's a test
     * source folder.
     *
     * @param module module containing this file
     * @param file   file location
     * @return boolean - true if in test source folder, false otherwise
     */
    protected static boolean isInTestSource(final @NotNull JpsModule module,
                                            final @NotNull File file) {
        final JpsModuleSourceRoot sourceRoot = JpsModelUtil.findSourceRootForFile(module, file);
        return sourceRoot != null && sourceRoot.getRootType().equals(JavaSourceRootType.TEST_SOURCE);
    }


    /**
     * Checks if given file has file extension for JAVA language.
     *
     * @param file name to be checked
     * @return boolean - true if java, false otherwise
     */
    private static boolean hasJavaFileExtension(final @NotNull File file) {
        final String fileNameLowerCase = file.getName().toLowerCase();
        for (final String extension : Language.Builtin.JAVA.getFileExtensions()) {
            if (fileNameLowerCase.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
