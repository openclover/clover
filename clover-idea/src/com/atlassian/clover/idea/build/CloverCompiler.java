package com.atlassian.clover.idea.build;

import clover.com.google.common.annotations.VisibleForTesting;
import clover.com.google.common.collect.ImmutableMap;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.RegexpContext;
import com.atlassian.clover.context.StatementRegexpContext;
import com.atlassian.clover.idea.CloverToolWindowId;
import com.atlassian.clover.idea.LibrarySupport;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.config.regexp.Regexp;
import com.atlassian.clover.idea.coverage.CoverageManager;
import com.atlassian.clover.idea.coverage.EventListenerInstallator;
import com.atlassian.clover.idea.util.CharsetUtil;
import com.atlassian.clover.idea.util.MiscUtils;
import com.atlassian.clover.idea.util.ProjectUtil;
import com.atlassian.clover.idea.util.tmp.TmpPathResolver;
import com.atlassian.clover.idea.util.ui.ExceptionDialog;
import com.atlassian.clover.idea.util.vfs.VfsUtil;
import com.atlassian.clover.instr.InstrumentationSessionImpl;
import com.atlassian.clover.instr.java.InstrumentationSource;
import com.atlassian.clover.instr.java.Instrumenter;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.RegistryFormatException;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.compiler.JavaSourceTransformingCompiler;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.LanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * The CloverCompiler provides the integration of Clover into the IDEA build
 * system via the JavaSourceTransformingCompiler.
 *
 * Since IDEA12: it also integrates with the external build process (clover-jps-plugin), listening
 * for updates from it.
 *
 * @see com.intellij.openapi.compiler.JavaSourceTransformingCompiler
 * @see com.atlassian.clover.idea.build.jps.CloverJavaBuilder
 * @see com.atlassian.clover.idea.build.jps.CloverJavaSourceTransformer
 */
public class CloverCompiler implements JavaSourceTransformingCompiler {

    /**
     * Compilation status listener for a build performed in the same process as IDEA IDE
     * (this build mode became deprecated in IDEA12 and will be removed in IDEA13 probably).
     */
    class InternalCompilationStatusListener implements CompilationStatusListener {
        /**
         * When compilation is finished, close instrumentation session from current Instrumenter
         * instance, reload registry and notify IDE about changes.
         * @param aborted  (unused)
         * @param errors   (unused)
         * @param warnings (unused)
         * @param compileContext (unused)
         */
        @Override
        public void compilationFinished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
            closeInstrumentationSession();
        }

        /**
         * Not interesting for us. Just a debug
         * @param outputRoot  output directory
         * @param relativePath path to file
         */
        public void fileGenerated(String outputRoot, String relativePath) {
            LOG.debug("Internal build - file generated: outputRoot=<" + outputRoot + "> relativePath=<" + relativePath + ">");
        }

        private void closeInstrumentationSession() {
            if (instr != null) {
                try {
                    if (!isCloverInstrumentationEnabled()) {
                        return;
                    }

                    final CoverageManager coverageManager = ProjectPlugin.getPlugin(project).getCoverageManager();
                    final Clover2Registry registry = ((InstrumentationSessionImpl) instr.getSession()).getRegistry();
                    coverageManager.lockRegistryForUpdate(registry);
                    try {
                        instr.endInstrumentation(true);
                        instr = null;
                        LOG.info("completed Instrumentation.");
                    } finally {
                        coverageManager.releaseUpdatedRegistry(registry);
                    }
                } catch (CloverException e) {
                    LOG.error(e);
                }
            }
        }
    }


    /**
     * Compilation status listener for a project build performed in the external process
     * (this feature is available since IDEA12).
     */
    class ExternalCompilationStatusListener implements CompilationStatusListener {
        /**
         * When compilation in external process is finished, force reload of the entire database and notify
         * IDE about changes.
         *
         * @param aborted (unused)
         * @param errors  (unused)
         * @param warnings (unused)
         * @param compileContext (unused)
         *
         * @see com.atlassian.clover.idea.build.CloverCompiler.InternalCompilationStatusListener#closeInstrumentationSession()
         */
        @Override
        public void compilationFinished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
            final Runnable updateRegistryTask = new Runnable() {
                @Override
                public void run() {
                    LOG.info("CLOVER: COMPILATION IN EXTERNAL BUILD PROCESS HAS FINISHED");
                    if (!isCloverInstrumentationEnabled()) {
                        return;
                    }

                    LOG.info("CLOVER: RELOADING DATABASE AND COVERAGE DATA");
                    final CoverageManager coverageManager = ProjectPlugin.getPlugin(project).getCoverageManager();
                    // we don't load registry from file and pass here as parameter, because CoverageManager will reload it
                    // note: in closeInstrumentationSession() we pass it as compilation is made in the same process
                    coverageManager.lockRegistryForUpdate(null);
                    coverageManager.releaseUpdatedRegistry(null);
                }
            };

            if (!ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeAndWait(updateRegistryTask, ModalityState.defaultModalityState());
            } else {
                updateRegistryTask.run();
            }
        }

        /**
         *Not interesting for us. Just a debug
         * @param outputRoot  output directory
         * @param relativePath path to file
         */
        public void fileGenerated(String outputRoot, String relativePath) {
            LOG.info("External build - file generated: outputRoot=<" + outputRoot + "> relativePath=<" + relativePath + ">");
        }
    }

    /**
     * Remove coverage data files
     */
    class CoverageDataCleanerTask implements CompileTask {
        private final Project project;

        public CoverageDataCleanerTask(Project project) {
            this.project = project;
        }

        @Override
        public boolean execute(CompileContext context) {
            if (context.isMake() || !isCloverInstrumentationEnabled()) {
                return true;
            }

            final VirtualFile[] files = context.getCompileScope().getFiles(null, false);
            final VirtualFile[] projectFiles = context.getProjectCompileScope().getFiles(null, false);

            if (files.length == projectFiles.length) {
                // heuristics to detect project recompile
                context.addMessage(CompilerMessageCategory.INFORMATION, "Clover: deleting coverage data.", null, -1, -1);
                instr = null; // need to recreate instrumenter after deleting the coverage db
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        ProjectPlugin.getPlugin(project).getCoverageManager().delete();
                    }
                }, ModalityState.defaultModalityState());
            } else {
                Logger.getInstance(CoverageDataCleanerTask.class.getName()).info(
                        MessageFormat.format("Coverage database not deleted: narrowed built detected ({0} of {1} files in the build scope)",
                                files.length, projectFiles.length));
            }
            return true;
        }

    }

    /**
     * Performs pre-compilation check whether initstring entered in Clover configuration is valid and the
     * directory is accessible. Aborts build otherwise.
     */
    class ValidateInitStringTask implements CompileTask {
        @Override
        public boolean execute(CompileContext context) {
            // skip check if Clover instrumentation is disabled
            if (!isCloverInstrumentationEnabled()) {
                return true;
            }

            // ensure that a valid initString has been specified
            final IdeaCloverConfig config = ProjectPlugin.getPlugin(project).getConfig();
            String initStr = config.getInitString();
            if (initStr == null || initStr.length() == 0) {
                Messages.showErrorDialog("Please specify a valid Clover initString property before continuing.", "Require Initialisation String.");
                return false;
            }

            // ensure that the directory into which the coverage database will be written exists
            final File initStringFile = new File(initStr);
            if (!initStringFile.getParentFile().exists()) {
                if (!initStringFile.getParentFile().mkdirs()) {
                    final String message = "Could not create coverage directory " + initStringFile.getParentFile();
                    LOG.error(message);
                    Messages.showErrorDialog(message, "Invalid Initialisation String.");
                    return false;
                }
            }

            // ok, continue the build
            return true;
        }

    }

    /**
     * Add the ".clover" working directory to the list of excluded source roots, if necessary.
     */
    public class ExcludeCloverWorkDirFromProjectTask implements CompileTask {

        @Override
        public boolean execute(CompileContext context) {
            MiscUtils.invokeWriteActionAndWait(new Runnable() {
                @Override
                public void run() {
                    File wksp = ProjectUtil.getProjectWorkspace(project);
                    if (wksp.exists()) {
                        ProjectUtil.excludeFromProject(project, LocalFileSystem.getInstance().refreshAndFindFileByIoFile(wksp));
                    }
                }
            });

            return true;
        }
    }

    /**
     * We've got a workaround for IDEA 13.x, which copies the clover-idea.jar to a temporary directory (java.io.tmpdir).
     * Normally copying happens when the "build with clover" is being toggled or project reloaded. However, someone
     * could wipe out his temporary directory in the meantime. So this task is to ensure that the JAR is not missing
     * just before start of the compilation.
     */
    public class RefreshCloverGlobalLibraryTask implements CompileTask {
        @Override
        public boolean execute(CompileContext compileContext) {
            // copy Clover JAR to a temporary location if needed (IDEA13 + JAR is not present or different)
            LibrarySupport.copyCloverJarIfNeccessary();
            return true;
        }
    }

    /**
     * Manages the status of the CloverCompiler.
     */
    class CloverCompilerStatus {
        private int count;

        void instrumenting(CompileContext context, VirtualFile file) {
            LOG.info("Instrumenting " + file);
            count++;
            Module currentModule = context.getModuleByFile(file);
            context.getProgressIndicator().setText2("Files: " + count + " - Module: " + currentModule.getName());
            context.getProgressIndicator().setText("Instrumenting " + file.getName());
        }

        void reset() {
            count = 0;
        }
    }

    /**
     * Logger
     */
    private final Logger LOG = Logger.getInstance(CloverCompiler.class.getName());

    /**
     * Project loaded in IDEA
     */
    private final Project project;

    /**
     * We want to manage the instance of the CloverInstrumenter, using it for
     * one 'complete' Compile Cycle. Why? Every time we create a new instance,
     * we trigger the downstream creation of a new coverage data file.
     */
    private Instrumenter instr;

    /**
     * Manages the status of the CloverCompiler.
     */
    private final CloverCompilerStatus status = new CloverCompilerStatus();

    /**
     * Listener for build events generated by the compiler running in the same process as IDE
     */
    private final CompilationStatusListener internalBuildListener;

    /**
     * Listener for build events generated by the compiler running in the external process
     */
    private final CompilationStatusListener externalBuildListener;

    /**
     * Create a new Compiler instance for the specified project.
     *
     * @param proj current project
     */
    public CloverCompiler(Project proj) {
        project = proj;
        internalBuildListener = new InternalCompilationStatusListener();
        registerInternalBuildListener(internalBuildListener);

        externalBuildListener = new ExternalCompilationStatusListener();
        registerExternalBuildListener(externalBuildListener);

        final CompilerManager compilerManager = CompilerManager.getInstance(project);
        compilerManager.addBeforeTask(new CoverageDataCleanerTask(project));
        compilerManager.addBeforeTask(new ValidateInitStringTask());
        compilerManager.addBeforeTask(new ExcludeCloverWorkDirFromProjectTask());
        compilerManager.addBeforeTask(new RefreshCloverGlobalLibraryTask());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getDescription() {
        return "Clover Compiler";
    }

    /**
     * The validateConfiguration method is called once BEFORE the compiler
     * is invoked. Any initialisation required by this compile is handled here.
     */
    @Override
    public boolean validateConfiguration(final CompileScope compileScope) {
        // if Clover is not enabled, just return without running any of the Clover-specific checks
        if (!isCloverInstrumentationEnabled()) {
            return true;
        }

        initCompiler();

        // verify that instrumenter can be initialized
        final IdeaCloverConfig config = ProjectPlugin.getPlugin(project).getConfig();
        return verifyInstrumenter(config);
    }


    /**
     * Perform cleanup on IDEA project closure
     */
    public void cleanup() {
        unregisterInternalBuildListener(internalBuildListener);
    }

    /**
     * Returns true if the specified file should be instrumented.
     */
    @Override
    public boolean isTransformable(VirtualFile file) {

        LOG.debug("isTransformable: " + file);
        if (!ProjectPlugin.getPlugin(project).getConfig().isBuildWithClover()) {
            return false;
        }

        InclusionDetector inclusion = ProjectInclusionDetector.processFile(project, file);

        if (inclusion.isCloverDisabled()) {
            return false;
        }

        // a) only want to deal with java source files
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
     * Adds Clover instrumentation into the <code>vf</code> file.
     *
     * @param context current compilation context
     * @param vf      a copy of original file to be transformed.
     * @param origVf  an original file (it is passed for reference purposes only)
     * @return true if transformation was successful, false on error
     */
    @Override
    public boolean transform(final CompileContext context, final VirtualFile vf, final VirtualFile origVf) {
        final Application app = ApplicationManager.getApplication();

        // instrument all of the items.
        return app.runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                LOG.debug("transform: " + vf + ", " + origVf);

                ServiceManager.getService(project, TmpPathResolver.class).registerMapping(origVf, vf.getPath());

                try {
                    // Initialise instrumentation handler.
                    final IdeaCloverConfig config = ProjectPlugin.getPlugin(project).getConfig();
                    final Instrumenter instrumenter = getInstrumenter(config);
                    final Module module = ModuleUtil.findModuleForFile(origVf, project);
                    final JavaInstrumentationConfig instrConfig = instrumenter.getConfig();

                    if (module != null) {
                        final String sourceLevel = sourceLevelString(LanguageLevelUtil.getEffectiveLanguageLevel(module));
                        LOG.debug("Source level " + sourceLevel);
                        instrConfig.setSourceLevel(sourceLevel);
                    } else {
                        LOG.info("Cannot determine module for " + origVf +
                                ", using previous source level " + instrConfig.getSourceLevel());
                    }

                    status.instrumenting(context, origVf);

                    final String fileEncoding = CharsetUtil.getFileEncoding(origVf);
                    final Writer writer;
                    if (fileEncoding == null) {
                        writer = new FileWriter(VfsUtil.convertToFile(vf));
                    } else {
                        writer = new OutputStreamWriter(new FileOutputStream(VfsUtil.convertToFile(vf)), fileEncoding);
                    }
                    final InstrumentationSource input = new VirtualFileInstrumentationSource(origVf);
                    instrumenter.instrument(input, writer, fileEncoding);

                    return Boolean.TRUE;

                } catch (Exception e) {
                    LOG.info("Exception caught during compile.", e);
                    context.addMessage(CompilerMessageCategory.ERROR, "CloverException: " + e.getMessage(), (origVf != null) ? origVf.getUrl() : null, -1, -1);
                    return Boolean.FALSE;
                }
            }
        });
    }


    private void notifyWarning(Project project, String message) {
        ToolWindowManager.getInstance(project).notifyByBalloon(CloverToolWindowId.TOOL_WINDOW_ID, MessageType.WARNING, message);
    }

    /**
     * Subscribe to {@link CompilerTopics#COMPILATION_STATUS} topic in {@link MessageBus}
     * @param compilationStatusListener listener
     */
    private void registerExternalBuildListener(final CompilationStatusListener compilationStatusListener) {
        EventListenerInstallator.install(project, CompilerTopics.COMPILATION_STATUS, compilationStatusListener);
    }

    private void registerInternalBuildListener(final CompilationStatusListener compilationStatusListener) {
        final CompilerManager compilerManager = CompilerManager.getInstance(project);
        compilerManager.addCompilationStatusListener(compilationStatusListener);
    }

    private void unregisterInternalBuildListener(final CompilationStatusListener compilationStatusListener) {
        CompilerManager.getInstance(project).removeCompilationStatusListener(compilationStatusListener);
    }

    private boolean verifyInstrumenter(IdeaCloverConfig config) {
        try {
            return getInstrumenter(config) != null;
        } catch (RegistryFormatException ex) {
            final String msg = "Clover was unable to instrument your source because of an error. Deleting the existing coverage database and trying again.";
            LOG.warn(msg, ex);
            notifyWarning(project, msg);
        } catch (CloverException ex) {
            if (ExceptionDialog.OK_EXIT_CODE != ExceptionDialog.showYesNoDialog(
                    project,
                    "Clover was unable to instrument your source because of the following error:",
                    "Delete the existing coverage database and try again?",
                    ex,
                    "Clover Instrumentation")) {
                return false;
            }
        }

        ProjectPlugin.getPlugin(project).getCoverageManager().delete();
        try {
            return getInstrumenter(config) != null;
        } catch (Exception ex) {
            final String msg = "Clover instrumenter failed to initialize (after deleting coverage database)";
            LOG.warn(msg, ex);
            ExceptionDialog.showOKDialog(project, msg, "", ex, "Initializing Clover Compiler");
            return false;
        }
    }

    private LanguageLevel findFirstLanguageLevel() {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        return modules.length > 0
                ? LanguageLevelUtil.getEffectiveLanguageLevel(modules[0])
                : highestLangSupportByClovAndIntellij();
    }

    private LanguageLevel highestLangSupportByClovAndIntellij() {
        int jdk18Index = LanguageLevel.JDK_1_8.ordinal();
        return LanguageLevel.values()[Math.min(LanguageLevel.values().length, jdk18Index)];
    }

    private static final Map<LanguageLevel, String> LANGUAGE_LEVEL_TO_STRING =
            new ImmutableMap.Builder<LanguageLevel, String>()
                    .put(LanguageLevel.JDK_1_8, "1.8")
                    .put(LanguageLevel.JDK_1_7, "1.7")
                    .put(LanguageLevel.JDK_1_6, "1.6")
                    .put(LanguageLevel.JDK_1_5, "1.5")
                    .put(LanguageLevel.JDK_1_4, "1.4")
                    .put(LanguageLevel.JDK_1_3, "1.3")
                    .build();

    @VisibleForTesting
    static String sourceLevelString(LanguageLevel languageLevel) {
        final String level = LANGUAGE_LEVEL_TO_STRING.get(languageLevel);
        return level != null ? level : "1.8";
    }

    private void initCompiler() {
        instr = null;
        status.reset();
    }

    private Instrumenter getInstrumenter(IdeaCloverConfig config) throws CloverException {
        if (instr == null) {
            JavaInstrumentationConfig instrConfig = BuildUtil.configureNewInstrumenter(config, project);
            // Initial configuration of the language level setting.
            // The one set here will be used only for log message, so it does not need to be that precise.
            // The actual language level used during instrumentation will be set for each transformed file
            // individually - see transform()
            final LanguageLevel lvl = findFirstLanguageLevel();
            instrConfig.setSourceLevel(sourceLevelString(lvl));

            instrConfig.setEncoding(CharsetUtil.getProjectDefaultEncoding());
            Instrumenter instrumenter = new Instrumenter(instrConfig);

            final Clover2Registry registry;
            try {
                final CloverDatabase currentDatabase = ProjectPlugin.getPlugin(project).getCoverageManager().getCoverage();
                registry = currentDatabase != null ?
                        currentDatabase.getRegistry() :
                        Clover2Registry.createOrLoad(new File(config.getInitString()), project.getName());
            } catch (IOException e) {
                throw new CloverException(e);
            }
            ContextStore contextRegistry = new ContextStore();
            @SuppressWarnings("unchecked")
            List<Regexp> regexpCtxList = config.getRegexpContexts();

            for (Regexp regexp : regexpCtxList) {
                if (regexp.isValid()) {
                    RegexpContext regexpContext = regexp.toContextSetting();
                    if (regexpContext instanceof MethodRegexpContext) {
                        contextRegistry.addMethodContext((MethodRegexpContext) regexpContext);
                    } else {
                        contextRegistry.addStatementContext((StatementRegexpContext) regexpContext);
                    }
                }
            }
            registry.setContextStore(contextRegistry);

            instrumenter.startInstrumentation(registry); // may throw exception here

            instr = instrumenter; // assign only when initialized without exception
        }
        return instr;
    }

    /**
     * This method returns true if Clover instrumentation is enabled.
     *
     * @return true if Clover instrumentation is enabled.
     */
    private boolean isCloverInstrumentationEnabled() {
        IdeaCloverConfig config = ProjectPlugin.getPlugin(project).getConfig();
        return config.isEnabled() && config.isBuildWithClover();
    }

}