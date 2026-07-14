package org.openclover.idea.build;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.idea.CloverToolWindowId;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.coverage.EventListenerInstallator;
import org.openclover.idea.util.MiscUtils;
import org.openclover.idea.util.ProjectUtil;
import org.openclover.idea.util.ui.ExceptionDialog;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.registry.RegistryFormatException;

import java.io.File;
import java.text.MessageFormat;

/**
 * Integrates OpenClover with the IDEA build system. The actual source instrumentation happens in the external build
 * process (JPS) via {@link org.openclover.idea.build.jps.CloverJavaBuilder} /
 * {@link org.openclover.idea.build.jps.CloverJavaSourceTransformer}. What this class does:
 * <ul>
 *     <li>registers pre-build {@link CompileTask}s (clean stale coverage, validate the initstring and
 *     coverage database, exclude the {@code .clover} work directory);</li>
 *     <li>listens on the {@link CompilerTopics#COMPILATION_STATUS} message-bus topic so that, once the
 *     external build finishes, the coverage database is reloaded and the IDE is notified.</li>
 * </ul>
 *
 * @see org.openclover.idea.build.jps.CloverJavaBuilder
 * @see org.openclover.idea.build.jps.CloverJavaSourceTransformer
 */
public class CloverCompiler {

    /**
     * Compilation status listener for a project build performed in the external process.
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
         */
        @Override
        public void compilationFinished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
            final Runnable updateRegistryTask = () -> {
                LOG.info("OPENCLOVER: COMPILATION IN EXTERNAL BUILD PROCESS HAS FINISHED");
                if (!isCloverInstrumentationEnabled()) {
                    return;
                }

                LOG.info("OPENCLOVER: RELOADING DATABASE AND COVERAGE DATA");
                final CoverageManager coverageManager = ProjectPlugin.getPlugin(project).getCoverageManager();
                // we don't load registry from file and pass here as parameter, because CoverageManager will reload it
                coverageManager.lockRegistryForUpdate(null);
                coverageManager.releaseUpdatedRegistry(null);
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

            // getFiles() iterates the project file index; since IDEA 2026 compile tasks run on a
            // background pooled thread (not the EDT) and this requires read access
            final int[] counts = ApplicationManager.getApplication().runReadAction((Computable<int[]>) () ->
                    new int[]{
                            context.getCompileScope().getFiles(null, false).length,
                            context.getProjectCompileScope().getFiles(null, false).length
                    });
            final int fileCount = counts[0];
            final int projectFileCount = counts[1];

            if (fileCount == projectFileCount) {
                // heuristics to detect project recompile
                context.addMessage(CompilerMessageCategory.INFORMATION, "OpenClover: deleting coverage data.", null, -1, -1);
                ApplicationManager.getApplication().invokeAndWait(
                        () -> ProjectPlugin.getPlugin(project).getCoverageManager().delete(),
                        ModalityState.defaultModalityState());
            } else {
                Logger.getInstance(CoverageDataCleanerTask.class.getName()).info(
                        MessageFormat.format("Coverage database not deleted: narrowed built detected ({0} of {1} files in the build scope)",
                                fileCount, projectFileCount));
            }
            return true;
        }

    }

    /**
     * Performs a pre-compilation check whether the initstring entered in the Clover configuration is valid,
     * the target directory is accessible and the existing coverage database can be loaded. Aborts the build
     * otherwise. If the coverage database turns out to be corrupt, offers to delete it and retry.
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
                showErrorDialog("Please specify a valid OpenClover initString property before continuing.",
                        "Require Initialisation String.");
                return false;
            }

            // ensure that the directory into which the coverage database will be written exists
            final File initStringFile = new File(initStr);
            if (!initStringFile.getParentFile().exists()) {
                if (!initStringFile.getParentFile().mkdirs()) {
                    final String message = "Could not create coverage directory " + initStringFile.getParentFile();
                    LOG.error(message);
                    showErrorDialog(message, "Invalid Initialisation String.");
                    return false;
                }
            }

            // ensure the existing coverage database is readable (offer to recover from a corrupt one)
            return verifyDatabase(config);
        }
    }

    /**
     * Add the ".clover" working directory to the list of excluded source roots, if necessary.
     */
    public class ExcludeCloverWorkDirFromProjectTask implements CompileTask {

        @Override
        public boolean execute(CompileContext context) {
            MiscUtils.invokeWriteActionAndWait(() -> {
                File wksp = ProjectUtil.getProjectWorkspace(project);
                if (wksp.exists()) {
                    ProjectUtil.excludeFromProject(project, LocalFileSystem.getInstance().refreshAndFindFileByIoFile(wksp));
                }
            });

            return true;
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

        externalBuildListener = new ExternalCompilationStatusListener();
        registerExternalBuildListener(externalBuildListener);

        final CompilerManager compilerManager = CompilerManager.getInstance(project);
        compilerManager.addBeforeTask(new CoverageDataCleanerTask(project));
        compilerManager.addBeforeTask(new ValidateInitStringTask());
        compilerManager.addBeforeTask(new ExcludeCloverWorkDirFromProjectTask());
    }

    /**
     * Perform cleanup on IDEA project closure. The external build listener is subscribed to the project
     * message bus with the project as its {@code Disposable} parent, so it is unsubscribed automatically
     * when the project is closed; nothing extra is required here.
     */
    public void cleanup() {
    }

    private void notifyWarning(Project project, String message) {
        // compile tasks run off the EDT; the tool window balloon must be shown from the EDT.
        // No result needs to be read back here, so dispatch asynchronously
        ApplicationManager.getApplication().invokeLater(() ->
                ToolWindowManager.getInstance(project).notifyByBalloon(CloverToolWindowId.TOOL_WINDOW_ID, MessageType.WARNING, message));
    }

    /**
     * Subscribe to {@link CompilerTopics#COMPILATION_STATUS} topic in {@link MessageBus}
     * @param compilationStatusListener listener
     */
    private void registerExternalBuildListener(final CompilationStatusListener compilationStatusListener) {
        EventListenerInstallator.install(project, CompilerTopics.COMPILATION_STATUS, compilationStatusListener);
    }

    /**
     * Verifies that the coverage database referenced by the initstring can be loaded (or freshly created).
     * If it is corrupt, offers to delete it and retry once. Runs from a {@link CompileTask} - i.e. off the
     * EDT - so any user-facing dialog is shown on the EDT via {@link ApplicationManager}.
     *
     * @param config current Clover configuration
     * @return true if the database is usable (possibly after recovery), false if the build should abort
     */
    private boolean verifyDatabase(IdeaCloverConfig config) {
        try {
            loadOrCreateRegistry(config);
            return true;
        } catch (RegistryFormatException ex) {
            final String msg = "OpenClover was unable to instrument your source because of an error. Deleting the existing coverage database and trying again.";
            LOG.warn(msg, ex);
            notifyWarning(project, msg);
        } catch (CloverException ex) {
            if (!showYesNoRecoveryDialog(ex)) {
                return false;
            }
        }

        // recover: delete the existing (corrupt) coverage database and try once more
        ProjectPlugin.getPlugin(project).getCoverageManager().delete();
        try {
            loadOrCreateRegistry(config);
            return true;
        } catch (Exception ex) {
            final String msg = "OpenClover instrumenter failed to initialize (after deleting coverage database)";
            LOG.warn(msg, ex);
            showOkDialog(msg, ex);
            return false;
        }
    }

    /**
     * Loads the coverage registry from the initstring location, creating a fresh one if it does not exist yet.
     * A {@link RegistryFormatException} (a {@link CloverException}) signals a corrupt database.
     */
    private void loadOrCreateRegistry(IdeaCloverConfig config) throws CloverException {
        try {
            Clover2Registry.createOrLoad(new File(config.getInitString()), project.getName());
        } catch (java.io.IOException e) {
            throw new CloverException(e);
        }
    }

    private boolean showYesNoRecoveryDialog(final CloverException ex) {
        final int[] exitCode = new int[1];
        ApplicationManager.getApplication().invokeAndWait(() ->
                exitCode[0] = ExceptionDialog.showYesNoDialog(
                        project,
                        "OpenClover was unable to instrument your source because of the following error:",
                        "Delete the existing coverage database and try again?",
                        ex,
                        "OpenClover Instrumentation"),
                ModalityState.defaultModalityState());
        return exitCode[0] == ExceptionDialog.OK_EXIT_CODE;
    }

    private void showOkDialog(final String msg, final Throwable ex) {
        ApplicationManager.getApplication().invokeAndWait(() ->
                ExceptionDialog.showOKDialog(project, msg, "", ex, "Initializing OpenClover Compiler"),
                ModalityState.defaultModalityState());
    }

    private void showErrorDialog(final String message, final String title) {
        // compile tasks run off the EDT; dialogs must be shown from the EDT
        ApplicationManager.getApplication().invokeAndWait(
                () -> Messages.showErrorDialog(message, title),
                ModalityState.defaultModalityState());
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
