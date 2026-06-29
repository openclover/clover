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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
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

            // All output dirs (own + deps) on classpath so cross-project class references resolve.
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
     * Returns all output directories for the project and its transitive project dependencies.
     * Handles per-source-folder outputs, default output, and cross-project source references
     * (kind="src" path="/OtherProject").
     */
    static List<File> resolveOutputDirs(IProject project) throws Exception {
        Set<File> dirs = new LinkedHashSet<>();
        collectOutputDirs(project, dirs, new HashSet<>());
        return new ArrayList<>(dirs);
    }

    private static void collectOutputDirs(IProject project, Set<File> dirs, Set<String> visited) throws Exception {
        if (!visited.add(project.getName())) {
            return;
        }
        IJavaProject javaProject = JavaCore.create(project);
        for (IClasspathEntry entry : javaProject.getRawClasspath()) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                IPath path = entry.getPath();
                if (path.segmentCount() >= 1 && !path.segment(0).equals(project.getName())) {
                    // Cross-project source reference: kind="src" path="/OtherProject[/subfolder]"
                    IProject dep = project.getWorkspace().getRoot().getProject(path.segment(0));
                    collectOutputDirs(dep, dirs, visited);
                    continue;
                }
                IPath out = entry.getOutputLocation();
                if (out != null) {
                    dirs.add(resolveOutputPath(project, out));
                }
            } else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                IProject dep = project.getWorkspace().getRoot().getProject(
                        entry.getPath().lastSegment());
                collectOutputDirs(dep, dirs, visited);
            }
        }
        dirs.add(resolveOutputPath(project, javaProject.getOutputLocation()));
    }

    static File resolveOutputPath(IProject project, IPath outputPath) {
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

    /**
     * Scans all output directories for top-level classes whose simple name starts or ends with
     * "Test"/"Tests" and that declare at least one @Test method.  The @Test check filters out
     * production classes whose names happen to match the pattern (e.g. AppClassEndingInTest).
     */
    private static List<String> discoverTestClasses(List<File> outputDirs) {
        List<String> candidates = new ArrayList<>();
        for (File dir : outputDirs) {
            if (dir.isDirectory()) {
                collectTestClasses(dir, dir, candidates);
            }
        }
        List<String> confirmed = new ArrayList<>();
        for (String className : candidates) {
            // The output dirs that actually contain this class's .class file
            if (hasJUnitTestMethods(outputDirs, className)) {
                confirmed.add(className);
            }
        }
        return confirmed;
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

    /**
     * Loads the class in a temporary URLClassLoader and checks for @org.junit.Test annotations.
     * Uses annotation type-name comparison to avoid classloader identity mismatches.
     * Returns true if we cannot inspect the class (safe default: let JUnit decide).
     */
    private static boolean hasJUnitTestMethods(List<File> outputDirs, String className) {
        try {
            URL[] urls = new URL[outputDirs.size()];
            for (int i = 0; i < outputDirs.size(); i++) {
                urls[i] = outputDirs.get(i).toURI().toURL();
            }
            try (URLClassLoader cl = new URLClassLoader(urls, TestRunner.class.getClassLoader())) {
                Class<?> clazz = cl.loadClass(className);
                for (Method m : clazz.getMethods()) {
                    for (Annotation a : m.getAnnotations()) {
                        if ("org.junit.Test".equals(a.annotationType().getName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception | Error e) {
            return true; // cannot inspect; pass through and let JUnit handle it
        }
    }
}
