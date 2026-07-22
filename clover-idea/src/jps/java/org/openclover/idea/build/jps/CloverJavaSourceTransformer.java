package org.openclover.idea.build.jps;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.java.JavaSourceTransformer;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.model.JpsEncodingConfigurationService;
import org.jetbrains.jps.model.JpsEncodingProjectConfiguration;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.LanguageLevel;
import org.openclover.core.cfg.instr.java.SourceLevel;
import org.openclover.core.instr.java.Instrumenter;
import org.openclover.core.util.trie.Node;
import org.openclover.idea.build.InclusionDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the IDEA's JavaSourceTransformer interface which add Clover instrumentation code. Instrumentation
 * is performed in memory.
 *
 * @see org.openclover.idea.build.CloverCompiler
 */
public class CloverJavaSourceTransformer extends JavaSourceTransformer {

    private final Logger LOG = Logger.getInstance(CloverJavaSourceTransformer.class.getName());

    private JpsProject jpsProject;

    /**
     * Returns true if the specified file should be instrumented.
     */
    public boolean isTransformable(final @NotNull File file) {
        final String skipReason = getSkipReason(file);
        if (skipReason != null) {
            LOG.info("OpenClover: not instrumenting " + file + " - " + skipReason);
            return false;
        } else {
            LOG.info("OpenClover: file is transformable and will be instrumented: " + file);
            return true;
        }
    }

    /**
     * Analyze the file and return a human-readable reason why it must NOT be instrumented, or
     * <code>null</code> if the file is eligible for instrumentation. The reason string is used for
     * diagnostic logging so a user can understand why a given source file was (not) instrumented.
     *
     * @param file source file to check
     * @return reason for skipping the file, or <code>null</code> when the file should be instrumented
     */
    @Nullable
    private String getSkipReason(final @NotNull File file) {
        // check if "build with clover" option was selected
        if (!JpsModelUtil.isBuildWithCloverEnabled(jpsProject)) {
            return "the 'Build project with OpenClover' option is disabled in project settings";
        }

        // analyze current file
        final InclusionDetector inclusion = JpsProjectInclusionDetector.processFile(jpsProject, file);
        if (inclusion.isCloverDisabled()) {
            return "instrumentation is disabled for this project";
        }

        // only want to deal with java source files
        if (inclusion.isNotJava()) {
            return "it is not a Java source file";
        }

        if (inclusion.isModuleExcluded()) {
            return "it belongs to a module excluded from instrumentation";
        }

        if (inclusion.isPatternExcluded()) {
            return "it matches an exclusion pattern (or does not match any inclusion pattern)";
        }

        if (inclusion.isInNoninstrumentedTestSources()) {
            return "it resides in test sources marked as non-instrumented";
        }

        if (!inclusion.isIncluded()) {
            return "it is not included by the current inclusion/exclusion configuration";
        }

        // no reason to skip the file
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param file         location of the file being transformed
     * @param charSequence content of the file
     * @return CharSequence - modified content
     * @throws TransformError in case when code instrumentation fails
     */
    @Override
    public CharSequence transform(final File file, final CharSequence charSequence) throws TransformError {
        // the builder must have started and published its compile context before any file is transformed;
        // if it hasn't (e.g. the CloverJavaBuilder was not loaded into the external build process), we
        // cannot instrument and must return the original source unchanged
        final CompileContext compileContext = CloverJavaBuilder.getInstance().getCompileContext();
        if (compileContext == null) {
            LOG.info("OpenClover: no active build context, returning source unchanged for " + file);
            return charSequence;
        }

        // get current project
        jpsProject = compileContext.getProjectDescriptor().getProject();

        if (isTransformable(file)) {
            try {
                final LanguageLevel level = getLanguageLevelForFile(file);
                final Instrumenter instrumenter = CloverJavaBuilder.getInstance().getInstrumenter();
                if (instrumenter == null) {
                    LOG.info("OpenClover: instrumenter is not available (instrumentation session not started), "
                            + "returning source unchanged for " + file);
                    return charSequence;
                }
                // TODO CLOV-1284 parallel build - this instrumenter is shared, make language level local
                final SourceLevel sourceLevel = languageLevelToSourceLevel(level);
                instrumenter.getConfig().setSourceLevel(sourceLevel);
                final JpsEncodingProjectConfiguration projectEncodingConfiguration = JpsEncodingConfigurationService.getInstance().getEncodingConfiguration(jpsProject);
                final String fileEncoding = projectEncodingConfiguration != null ? projectEncodingConfiguration.getEncoding(file) : null;

                final CharSequence instrumentedCharSequence = instrumenter.instrument(file, charSequence, fileEncoding);
                final String message = "OpenClover: instrumenting " + file.getName() + " with source level " + sourceLevel;
                CloverJavaBuilder.getInstance().sendCompilerMessageToIDE(BuildMessage.Kind.PROGRESS, message);
                debugTransform(file, charSequence, instrumentedCharSequence, level);

                return instrumentedCharSequence;
            } catch (Exception ex) {
                throw new CloverInstrumentationException(ex);
            }
        } else {
            // isTransformable() already logged the concrete reason for skipping this file
            // return original sequence
            return charSequence;
        }
    }

    /**
     * Return Java language level for given source file. Detection is based on language level settings
     * for project and modules.
     * @param sourceFile location or the source file
     * @return LanguageLevel
     */
    public LanguageLevel getLanguageLevelForFile(final File sourceFile) {
        // search for a closest source root in our cache
        final Node<String, LanguageLevel> closestSourceRoot = CloverJavaBuilder.getInstance()
                .getSourceRootToLanguageLevel().findNearest(sourceFile);
        // not found? assume the latest supported
        return (closestSourceRoot.getValue() != null ? closestSourceRoot.getValue() : LanguageLevel.JDK_1_9);
    }

    // Converts org.jetbrains.jps.model.java.LanguageLevel to Clover's SourceLevel. This is the single
    // source of truth for the mapping; the in-process CloverCompiler copy was removed together with the
    // retired JavaSourceTransformingCompiler path.
    protected static final Map<LanguageLevel, SourceLevel> LANGUAGE_LEVEL_TO_SOURCE_LEVEL
            = new HashMap<LanguageLevel, SourceLevel>() {{
        // Java 7 and below are instrumented at the lowest source level Clover supports (Java 8)
        put(LanguageLevel.JDK_1_3, SourceLevel.JAVA_8);
        put(LanguageLevel.JDK_1_4, SourceLevel.JAVA_8);
        put(LanguageLevel.JDK_1_5, SourceLevel.JAVA_8);
        put(LanguageLevel.JDK_1_6, SourceLevel.JAVA_8);
        put(LanguageLevel.JDK_1_7, SourceLevel.JAVA_8);
        put(LanguageLevel.JDK_1_8, SourceLevel.JAVA_8);
        put(LanguageLevel.JDK_1_9, SourceLevel.JAVA_9);
        put(LanguageLevel.JDK_10, SourceLevel.JAVA_10);
        put(LanguageLevel.JDK_11, SourceLevel.JAVA_11);
        put(LanguageLevel.JDK_12, SourceLevel.JAVA_12);
        put(LanguageLevel.JDK_13, SourceLevel.JAVA_13);
        put(LanguageLevel.JDK_14, SourceLevel.JAVA_14);
        put(LanguageLevel.JDK_15, SourceLevel.JAVA_15);
        put(LanguageLevel.JDK_16, SourceLevel.JAVA_16);
        put(LanguageLevel.JDK_17, SourceLevel.JAVA_17);
        put(LanguageLevel.JDK_18, SourceLevel.JAVA_18);
        put(LanguageLevel.JDK_19, SourceLevel.JAVA_19);
        put(LanguageLevel.JDK_20, SourceLevel.JAVA_20);
        put(LanguageLevel.JDK_21, SourceLevel.JAVA_21);
        // Java 22-25 are resolved reflectively: the LanguageLevel enum constants JDK_22..JDK_25 do not
        // exist in older IntelliJ SDKs (e.g. 2024.3) that this module may be compiled against, so a
        // direct reference would break the build. Any constant that is missing at runtime is simply
        // skipped and handled by the fallback in languageLevelToSourceLevel().
        putIfPresent(this, "JDK_22", SourceLevel.JAVA_22);
        putIfPresent(this, "JDK_23", SourceLevel.JAVA_23);
        putIfPresent(this, "JDK_24", SourceLevel.JAVA_24);
        putIfPresent(this, "JDK_25", SourceLevel.JAVA_25);
    }};

    /**
     * Reflectively resolve a {@link LanguageLevel} enum constant by name and, when present in the
     * current IntelliJ SDK, register its mapping. Constants absent from the running SDK are ignored.
     */
    private static void putIfPresent(final Map<LanguageLevel, SourceLevel> map,
                                     final String languageLevelName, final SourceLevel sourceLevel) {
        try {
            map.put(LanguageLevel.valueOf(languageLevelName), sourceLevel);
        } catch (IllegalArgumentException notInThisSdk) {
            // this IntelliJ SDK predates the given JDK language level - rely on the fallback
        }
    }

    /**
     * Convert IDEA's LanguageLevel to ours SourceLevel
     */
    public SourceLevel languageLevelToSourceLevel(final LanguageLevel level) {
        // If the map has no entry then a language level newer than Clover's highest supported
        // (Java 25) - or a preview/experimental level such as JDK_25_PREVIEW or JDK_X - was
        // requested; instrument at the highest level we know about.
        final SourceLevel sourceLevel = LANGUAGE_LEVEL_TO_SOURCE_LEVEL.get(level);
        return sourceLevel == null ? SourceLevel.JAVA_25 : sourceLevel;
    }

    private void debugTransform(final File file, final CharSequence charSequence,
                                final CharSequence instrCharSequence, final LanguageLevel level) {
        if (JpsModelUtil.isDumpInstrumentedSources(jpsProject)) {
            final String message = "CloverSourceTransformer.transform(" + file.getAbsolutePath()
                    + ", with " + charSequence.length() + "characters) called; language level=" + level;
            LOG.debug(message);

            String tmpDir;
            try {
                tmpDir = System.getProperty("java.io.tmpdir");
            } catch (SecurityException ex) {
                tmpDir = null;
            }

            if (tmpDir != null) {
                writeCharSequenceToFile(new File(tmpDir, file.getName() + ".orig"), charSequence);
                writeCharSequenceToFile(new File(tmpDir, file.getName() + ".instr"), instrCharSequence);
            } else {
                LOG.debug("Unable to read java.io.tmpdir property. Temporary files with instrumented code will not be written.");
            }
        }
    }

    private void writeCharSequenceToFile(final File file, final CharSequence charSequence) {
        try {
            final Writer out = new OutputStreamWriter(new FileOutputStream(file));
            out.append(charSequence);
            out.close();
        } catch (IOException ex) {
            LOG.debug("Unable to write temporary file: " + ex.getMessage());
        }
    }
}
