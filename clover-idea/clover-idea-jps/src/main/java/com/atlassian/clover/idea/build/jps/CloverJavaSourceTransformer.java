package com.atlassian.clover.idea.build.jps;

import clover.antlr.RecognitionException;
import clover.antlr.TokenStreamException;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.instr.java.Instrumenter;
import com.atlassian.clover.idea.build.InclusionDetector;
import com.atlassian.clover.util.trie.PrefixTree;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.java.JavaSourceTransformer;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.model.JpsEncodingConfigurationService;
import org.jetbrains.jps.model.JpsEncodingProjectConfiguration;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.LanguageLevel;

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
 * @see com.atlassian.clover.idea.build.CloverCompiler
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
                instrumenter.getConfig().setSourceLevel(languageLevelToShortString(level));
                final JpsEncodingProjectConfiguration projectEncodingConfiguration = JpsEncodingConfigurationService.getInstance().getEncodingConfiguration(jpsProject);
                final String fileEncoding = projectEncodingConfiguration != null ? projectEncodingConfiguration.getEncoding(file) : null;

                final CharSequence instrumentedCharSequence = instrumenter.instrument(file, charSequence, fileEncoding);
                final String message = "Clover: instrumenting " + file.getName() + " with language level " + level;
                CloverJavaBuilder.getInstance().sendCompilerMessageToIDE(BuildMessage.Kind.PROGRESS, message);
                debugTransform(file, charSequence, instrumentedCharSequence, level);

                return instrumentedCharSequence;
            } catch (TokenStreamException ex) {
                throw new CloverInstrumentationException(ex);
            } catch (IOException ex) {
                throw new CloverInstrumentationException(ex);
            } catch (RecognitionException ex) {
                throw new CloverInstrumentationException(ex);
            } catch (CloverException ex) {
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
        final PrefixTree.Node<String, LanguageLevel> closestSourceRoot = CloverJavaBuilder.getInstance()
                .getSourceRootToLanguageLevel().findNearest(sourceFile);
        // not found? assume the latest supported
        return (closestSourceRoot.getValue() != null ? closestSourceRoot.getValue() : LanguageLevel.JDK_1_8);
    }

    protected final Map<LanguageLevel, String> languageLevel2VersionString = new HashMap<LanguageLevel, String>() {{
        put(LanguageLevel.JDK_1_3, "1.3");
        put(LanguageLevel.JDK_1_4, "1.4");
        put(LanguageLevel.JDK_1_5, "1.5");
        put(LanguageLevel.JDK_1_6, "1.6");
        put(LanguageLevel.JDK_1_7, "1.7");
        put(LanguageLevel.JDK_1_8, "1.8");
    }};

    public String languageLevelToShortString(final LanguageLevel level) {
        return languageLevel2VersionString.containsKey(level)
                ? languageLevel2VersionString.get(level)
                : languageLevel2VersionString.get(LanguageLevel.JDK_1_8); // use 1.8 if key not found
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
