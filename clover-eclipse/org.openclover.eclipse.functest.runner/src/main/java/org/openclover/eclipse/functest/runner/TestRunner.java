package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.openclover.eclipse.core.projects.CloverProject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Launches JUnit 4 tests in a subprocess using the same JVM that runs Eclipse. */
public class TestRunner {

    public static void run(IProject project, String cloverRuntime, File ignored, TestResult result) {
        try {
            List<File> outputDirs = resolveOutputDirs(project);
            File junitJar    = findPluginJar("org.junit_4");
            File hamcrestJar = findPluginJar("org.hamcrest_");
            String initString = CloverProject.getFor(project).getRegistryFile().getAbsolutePath();
            List<String> testClasses = discoverTestClasses(outputDirs);

            if (testClasses.isEmpty()) {
                return;
            }

            String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

            // All output dirs on classpath so cross-folder dependencies resolve (e.g. appbin→testbin).
            StringBuilder cp = new StringBuilder();
            for (File dir : outputDirs) {
                if (cp.length() > 0) cp.append(File.pathSeparator);
                cp.append(dir.getAbsolutePath());
            }
            cp.append(File.pathSeparator).append(cloverRuntime);
            cp.append(File.pathSeparator).append(junitJar.getAbsolutePath());
            cp.append(File.pathSeparator).append(hamcrestJar.getAbsolutePath());

            List<String> cmd = new ArrayList<>();
            cmd.add(javaExe);
            cmd.add("-cp");
            cmd.add(cp.toString());
            cmd.add("org.junit.runner.JUnitCore");
            cmd.addAll(testClasses);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("CLOVER_INITSTRING", initString);
            pb.redirectErrorStream(true);

            WorkspaceManager.log("Launching JUnit subprocess for " + project.getName() + ": " + String.join(" ", cmd));
            Process proc = pb.start();
            WorkspaceManager.log("JUnit subprocess started (pid=" + proc.pid() + "); reading output...");
            String output = new String(proc.getInputStream().readAllBytes());
            int exit = proc.waitFor();
            WorkspaceManager.log("JUnit subprocess finished for " + project.getName() + " (exit=" + exit + ")");

            if (exit != 0) {
                result.fail("JUnit runner exited " + exit + ":\n" + output);
            } else {
                WorkspaceManager.log("JUnit output for " + project.getName() + ":\n" + output);
            }
        } catch (Exception e) {
            result.fail("TestRunner error: " + e.getMessage());
        }
    }

    /**
     * Returns all output directories declared in the project's classpath:
     * per-source-folder outputs plus the default output. Handles the case
     * where output path="" means the project root is the output directory.
     */
    private static List<File> resolveOutputDirs(IProject project) throws Exception {
        IJavaProject javaProject = JavaCore.create(project);
        Set<File> dirs = new LinkedHashSet<>();

        for (IClasspathEntry entry : javaProject.getRawClasspath()) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                IPath out = entry.getOutputLocation();
                if (out != null) {
                    dirs.add(resolveOutputPath(project, out));
                }
            }
        }
        dirs.add(resolveOutputPath(project, javaProject.getOutputLocation()));

        return new ArrayList<>(dirs);
    }

    private static File resolveOutputPath(IProject project, IPath outputPath) {
        if (outputPath.segmentCount() <= 1) {
            // output="" means the project root itself is the output directory
            return project.getLocation().toFile();
        }
        return project.getWorkspace().getRoot()
                .getFolder(outputPath).getLocation().toFile();
    }

    private static File findPluginJar(String prefix) throws IOException, URISyntaxException {
        File pluginsDir = new File(Platform.getInstallLocation().getURL().toURI())
                .toPath().resolve("plugins").toFile();
        if (pluginsDir.isDirectory()) {
            File[] candidates = pluginsDir.listFiles(
                    f -> f.getName().startsWith(prefix) && f.getName().endsWith(".jar"));
            if (candidates != null && candidates.length > 0) {
                return candidates[0];
            }
        }
        throw new IOException("Could not find " + prefix + "*.jar in " + pluginsDir);
    }

    /** Scans all output directories for top-level classes whose simple name starts or ends with "Test"/"Tests". */
    private static List<String> discoverTestClasses(List<File> outputDirs) {
        List<String> classes = new ArrayList<>();
        for (File dir : outputDirs) {
            if (dir.isDirectory()) {
                collectTestClasses(dir, dir, classes);
            }
        }
        return classes;
    }

    private static void collectTestClasses(File root, File dir, List<String> classes) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File f : entries) {
            if (f.isDirectory()) {
                collectTestClasses(root, f, classes);
            } else if (f.isFile() && f.getName().endsWith(".class")) {
                String name = f.getName();
                // Skip inner/synthetic classes — Clover adds $__CLR... synthetics that confuse JUnit
                if (name.contains("$")) {
                    continue;
                }
                if (name.startsWith("Test") || name.endsWith("Test.class") || name.endsWith("Tests.class")) {
                    String relative = root.toURI().relativize(f.toURI()).getPath();
                    String className = relative.substring(0, relative.length() - 6).replace('/', '.');
                    classes.add(className);
                }
            }
        }
    }
}
