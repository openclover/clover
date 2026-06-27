package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.openclover.eclipse.core.projects.CloverProject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** Launches JUnit 4 tests in a subprocess using the same JVM that runs Eclipse. */
public class TestRunner {

    public static void run(IProject project, String cloverRuntime, File ignored, TestResult result) {
        try {
            File outputDir = resolveOutputDir(project);
            File junitJar   = findJUnitJar();
            String initString = CloverProject.getFor(project).getRegistryFile().getAbsolutePath();
            List<String> testClasses = discoverTestClasses(outputDir);

            if (testClasses.isEmpty()) {
                return;
            }

            String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String cp = outputDir.getAbsolutePath()
                    + File.pathSeparator + cloverRuntime
                    + File.pathSeparator + junitJar.getAbsolutePath();

            List<String> cmd = new ArrayList<>();
            cmd.add(javaExe);
            cmd.add("-cp");
            cmd.add(cp);
            cmd.add("org.junit.runner.JUnitCore");
            cmd.addAll(testClasses);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("CLOVER_INITSTRING", initString);
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            int exit = proc.waitFor();

            if (exit != 0) {
                result.fail("JUnit runner exited " + exit + ":\n" + output);
            } else {
                System.out.println("[runner] JUnit output for " + project.getName() + ":\n" + output);
            }
        } catch (Exception e) {
            result.fail("TestRunner error: " + e.getMessage());
        }
    }

    private static File resolveOutputDir(IProject project) throws Exception {
        IPath outputPath = JavaCore.create(project).getOutputLocation();
        return project.getWorkspace().getRoot()
                .getFolder(outputPath).getLocation().toFile();
    }

    private static File findJUnitJar() throws IOException, URISyntaxException {
        File pluginsDir = new File(Platform.getInstallLocation().getURL().toURI())
                .toPath().resolve("plugins").toFile();
        if (pluginsDir.isDirectory()) {
            File[] candidates = pluginsDir.listFiles(
                    f -> f.getName().startsWith("org.junit_4") && f.getName().endsWith(".jar"));
            if (candidates != null && candidates.length > 0) {
                return candidates[0];
            }
        }
        throw new IOException("Could not find org.junit_4*.jar in " + pluginsDir);
    }

    /** Scans the output directory for classes matching *Test.class or *Tests.class. */
    private static List<String> discoverTestClasses(File outputDir) {
        List<String> classes = new ArrayList<>();
        collectTestClasses(outputDir, outputDir, classes);
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
                if (name.endsWith("Test.class") || name.endsWith("Tests.class")) {
                    String relative = root.toURI().relativize(f.toURI()).getPath();
                    // strip .class, replace / with .
                    String className = relative.substring(0, relative.length() - 6).replace('/', '.');
                    classes.add(className);
                }
            }
        }
    }
}
