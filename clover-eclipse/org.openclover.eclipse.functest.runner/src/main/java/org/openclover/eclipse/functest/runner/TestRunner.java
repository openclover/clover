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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
            // Eclipse 2024+ ships org.hamcrest_3.0.0.jar; older versions ship org.hamcrest.core_*.jar.
            // Either way hamcrest may also be bundled inside the junit jar, so treat it as optional.
            File hamcrestJar = findPluginJarOptional("org.hamcrest_", "org.hamcrest.core_");
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
            if (hamcrestJar != null) {
                cp.append(File.pathSeparator).append(hamcrestJar.getAbsolutePath());
            }

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
        File jar = findPluginJarOptional(prefix);
        if (jar == null) {
            File pluginsDir = new File(Platform.getInstallLocation().getURL().toURI())
                    .toPath().resolve("plugins").toFile();
            throw new IOException("Could not find " + prefix + "*.jar in " + pluginsDir);
        }
        return jar;
    }

    /** Returns the first matching jar for any of the given prefixes, or null if none found. */
    private static File findPluginJarOptional(String... prefixes) throws URISyntaxException {
        File pluginsDir = new File(Platform.getInstallLocation().getURL().toURI())
                .toPath().resolve("plugins").toFile();
        if (pluginsDir.isDirectory()) {
            for (String prefix : prefixes) {
                File[] candidates = pluginsDir.listFiles(
                        f -> f.getName().startsWith(prefix) && f.getName().endsWith(".jar"));
                if (candidates != null && candidates.length > 0) {
                    return candidates[0];
                }
            }
        }
        return null;
    }

    /**
     * Scans all output directories for top-level classes whose simple name starts or ends with
     * "Test"/"Tests" and whose bytecode contains the @Test annotation descriptor.
     * The bytecode check filters out production classes whose names happen to match the
     * pattern (e.g. AppClassEndingInTest) without any classloader dependency.
     */
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
                    if (hasJUnitTestAnnotation(f)) {
                        String relative = root.toURI().relativize(f.toURI()).getPath();
                        String className = relative.substring(0, relative.length() - 6).replace('/', '.');
                        classes.add(className);
                    }
                }
            }
        }
    }

    /**
     * Scans the compiled .class file's constant pool for the @Test annotation descriptor
     * "Lorg/junit/Test;" — present whenever a method is annotated with @Test.
     * ISO-8859-1 gives a 1-to-1 byte→char mapping so String.contains works on raw bytes.
     * Returns true (safe default) if the file cannot be read.
     */
    private static boolean hasJUnitTestAnnotation(File classFile) {
        try {
            String constantPool = new String(Files.readAllBytes(classFile.toPath()), StandardCharsets.ISO_8859_1);
            return constantPool.contains("Lorg/junit/Test;");
        } catch (Exception e) {
            return true;
        }
    }
}
