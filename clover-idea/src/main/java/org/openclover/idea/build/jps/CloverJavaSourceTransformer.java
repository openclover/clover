package org.openclover.idea.build.jps;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.java.JavaSourceTransformer;
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
    public boolean isTransformable(final @NotNull File file) throws TransformError {
        LOG.debug("isTransformable: " + file);

        // check if "build with clover" option was selected
        if (!JpsModelUtil.isBuildWithCloverEnabled(jpsProject)) {
            return false;
        }

        // analyze current file
        final InclusionDetector inclusion = JpsProjectInclusionDetector.processFile(jpsProject, file);
        if (inclusion.isCloverDisabled()) {
            LOG.debug("Instrumentation is disabled");
            return false;
        }

        // only want to deal with java source files
        if (inclusion.isNotJava()) {
            LOG.debug("Ignoring non-java file: " + file);
            return false;
        }
        if (inclusion.isModuleExcluded()) {
            LOG.debug("File belongs to excluded module: " + file);
            return false;
        }

        if (inclusion.isPatternExcluded()) {
            LOG.debug("Ignoring excluded file: " + file);
            return false;
        }

        if (inclusion.isInNoninstrumentedTestSources()) {
            LOG.debug("Ignoring marked test case: " + file);
            return false;
        }

        return inclusion.isIncluded();
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
        // get current project
        jpsProject = CloverJavaBuilder.getInstance().getCompileContext().getProjectDescriptor().getProject();

        if (isTransformable(file)) {
            try {
                final LanguageLevel level = getLanguageLevelForFile(file);
                final Instrumenter instrumenter = CloverJavaBuilder.getInstance().getInstrumenter();
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
            LOG.info("CloverSourceTransformer.transform skipping file " + file);
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
    }};

    /**
     * Convert IDEA's LanguageLevel to ours SourceLevel
     */
    public SourceLevel languageLevelToSourceLevel(final LanguageLevel level) {
        // If the map has no entry then a language level newer than Clover's highest supported
        // (Java 17) was requested - instrument at the highest level we know about.
        final SourceLevel sourceLevel = LANGUAGE_LEVEL_TO_SOURCE_LEVEL.get(level);
        return sourceLevel == null ? SourceLevel.JAVA_17 : sourceLevel;
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
