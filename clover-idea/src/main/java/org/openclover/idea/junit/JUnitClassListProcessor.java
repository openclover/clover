package org.openclover.idea.junit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.optimization.Optimizable;
import org.openclover.core.api.optimization.OptimizationOptions;
import org.openclover.core.api.optimization.StringOptimizable;
import org.openclover.core.optimization.LocalSnapshotOptimizer;
import org.openclover.core.optimization.OptimizationSession;
import org.openclover.core.optimization.Snapshot;
import org.openclover.idea.CloverToolWindowId;
import org.openclover.idea.ProjectPluginViaReflection;
import org.openclover.idea.coverage.SnapshotFileMutex;
import org.openclover.idea.junit.config.OptimizedConfigurationSettings;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.IOStreamUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

public class JUnitClassListProcessor {
    protected final File ideaGeneratedFile;
    protected final SavingsReporter savingsReporter;
    protected final Project currentProject;
    protected final OptimizedConfigurationSettings optimizationSettings;
    static final String TMP_FILE_PREFIX = "idea_optimized_junit";
    static final String TMP_FILE_EXT = ".tmp";

    public JUnitClassListProcessor(SavingsReporter savingsReporter, File ideaGeneratedFile, Project currentProject, OptimizedConfigurationSettings optimizationSettings) {
        this.savingsReporter = savingsReporter;
        this.ideaGeneratedFile = ideaGeneratedFile;
        this.currentProject = currentProject;
        this.optimizationSettings = optimizationSettings;
    }

    boolean processFile() {
        final List<String> header = newArrayList();
        final List<Optimizable> underTest;
        try {
            underTest = readOptimizables(ideaGeneratedFile, header);
        } catch (IOException e) {
            Logger.getInstance().warn("Error reading JUnit test manifest file", e);
            return false;
        }
        if (underTest == null) {
            return false;
        }
        final OptimizationSession[] sessionHolder = new OptimizationSession[1];

        final Collection<Optimizable> optimized = optimize(currentProject, optimizationSettings, underTest, sessionHolder);
        if (optimized.size() == 0) {
            if (!ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeLater(NoTestsFoundNotification::showNotifications);
            } else {
                NoTestsFoundNotification.showNotifications();
            }
        }
        writeManifestFile(ideaGeneratedFile, header.toArray(new String[header.size()]), optimized);
        savingsReporter.reportSavings(currentProject, sessionHolder[0]);
        return true;
    }

    @NotNull
    Collection<Optimizable> optimize(@NotNull final Project project, @NotNull OptimizedConfigurationSettings settings, @NotNull List<Optimizable> optimizables, OptimizationSession[] sessionHolder) {
        final CloverDatabase database = ProjectPluginViaReflection.getPlugin(project).getCoverageManager().getCoverage();
        if (database == null) {
            final String msg = "OpenClover coverage database not found for project '" + project.getName() + "'\n\n"
                    + "Have you enabled Build with OpenClover on this project?";
            Logger.getInstance().info(msg);
            ApplicationManager.getApplication().invokeLater(() ->
                    ToolWindowManager
                            .getInstance(project)
                            .notifyByBalloon(CloverToolWindowId.TOOL_WINDOW_ID, MessageType.WARNING, msg));
            return optimizables;
        }

        final String initString = database.getInitstring();
        final OptimizationOptions optimizationOptions =
                new OptimizationOptions.Builder()
                        .optimizableName("class")
                        .initString(initString)
                        .reorder(settings.getReorder())
                        .maxCompilesBeforeStaleSnapshot(settings.isDiscardSnapshots() ? settings.getCompilesBeforeStaleSnapshot() : Integer.MAX_VALUE)
                        .minimize(settings.isMinimize()).build();

        final Snapshot snapshot;
        final SnapshotFileMutex mutex = ServiceManager.getService(SnapshotFileMutex.class);
        final File snapshotFile = new File(Snapshot.fileNameForInitString(initString));
        mutex.lockFile(snapshotFile);
        try {
            snapshot = Snapshot.loadFrom(snapshotFile);
        } finally {
            mutex.releaseFile(snapshotFile);
        }
        if (snapshot == null) {
            // create new snapshot file using data from clover database
            try {
                Snapshot.generateFor(database).store();
            } catch (IOException e) {
                Logger.getInstance().warn("Cannot store snapshot file, following test run will not be optimized", e);
            }
            return optimizables;
        }


        final LocalSnapshotOptimizer optimizer = new LocalSnapshotOptimizer(snapshot, database.getRegistry(), optimizationOptions);
        if (!optimizer.canOptimize()) {
            Logger.getInstance().info("Cannot optimize: " + optimizer.cannotOptimizeCause());
            if (optimizer.isTooStale()) {
                snapshot.delete();
            }
            return optimizables;
        }

        sessionHolder[0] = new OptimizationSession(optimizationOptions);
        @SuppressWarnings({"unchecked"}) final List<Optimizable> optimized = optimizer.optimize(optimizables, sessionHolder[0]);
        return optimized;
    }

    /**
     * Read lines from the file and wrap them as Optimizable adapter
     *
     * @param tmpFile      file to read lines from
     * @param headerHolder first 2 lines of the file would go there
     * @return list of optimizables or null if the file cannot be run
     * @throws java.io.IOException when reading the temporary manifest file
     */
    @Nullable
    static List<Optimizable> readOptimizables(@NotNull File tmpFile, @NotNull List<String> headerHolder) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(tmpFile));

            String line = reader.readLine();

            if (line == null) {
                return null;
            } else if (line.startsWith("-junit")) {
                headerHolder.add(line);
                line = reader.readLine();
            }

            if (line == null) {
                return null;
            }

            // package name or empty
            headerHolder.add(line);

            final List<Optimizable> optimizables = newArrayList();

            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                // IDEA14 may return two empty lines at the beginning
                if (s.isEmpty()) {
                    headerHolder.add(s);
                } else {
                    optimizables.add(new StringOptimizable(s));
                }
            }
            return optimizables;
        } finally {
            IOStreamUtils.close(reader);
        }

    }

    static boolean writeManifestFile(@NotNull File file, @NotNull String[] header, @NotNull Collection<Optimizable> optimized) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            for (String line : header) {
                writer.println(line);
            }

            for (Optimizable optimizable : optimized) {
                writer.println(optimizable.getName());
            }
        } catch (IOException e) {
            Logger.getInstance().error("Cannot write to JUnit test manifest file", e);
            return false;
        } finally {
            IOStreamUtils.close(writer);
        }
        return true;
    }

    static void moveFile(File src, File dest) {
        if (!src.renameTo(dest)) {
            //noinspection ResultOfMethodCallIgnored
            dest.delete(); // renameTo may fail if the dest file exists. FileUtil will retry renameTo
            try {
                FileUtil.rename(src, dest);
            } catch (IOException e) {
                Logger.getInstance().error("Cannot prepare optimized test list for JUnit runner", e);
            }
        }
    }
}
