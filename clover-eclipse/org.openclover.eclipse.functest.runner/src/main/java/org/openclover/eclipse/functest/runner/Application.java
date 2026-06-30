package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Eclipse IApplication entry point for functional tests.
 * Invoked headlessly via:
 *   eclipse -nosplash -application org.openclover.eclipse.functest.runner.application
 *           -projectsDir &lt;dir&gt; -cloverRuntime &lt;jar&gt; -reportsDir &lt;dir&gt;
 */
public class Application implements IApplication {

    private static final Integer EXIT_ERROR = 1;

    /** Tier 3 projects: not imported or built; reported as skipped in XML output. */
    private static final Map<String, String> SKIPPED_PROJECTS = new LinkedHashMap<>();
    static {
        SKIPPED_PROJECTS.put("TestAntBuild",          "Tier 3: requires Ant on PATH");
        SKIPPED_PROJECTS.put("TestDynamicWebProject", "Tier 3: requires WTP bundles not present in Eclipse for Java");
        SKIPPED_PROJECTS.put("TestEquinoxProject",    "Tier 3: OSGi classpath complications");
        SKIPPED_PROJECTS.put("TestEquinoxTestsProject","Tier 3: OSGi classpath complications");
    }

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        File projectsDir = arg(args, "-projectsDir");
        File reportsDir  = arg(args, "-reportsDir");
        String cloverRuntime = argString(args, "-cloverRuntime");
        String eclipseVersion = argString(args, "-eclipseVersion");
        if (eclipseVersion == null) {
            eclipseVersion = "unknown";
        }

        if (projectsDir == null || cloverRuntime == null || reportsDir == null) {
            System.err.println("Usage: -projectsDir <dir> -cloverRuntime <jar> -reportsDir <dir> [-eclipseVersion <ver>]");
            return EXIT_ERROR;
        }
        reportsDir.mkdirs();

        WorkspaceManager.log("projectsDir   = " + projectsDir);
        WorkspaceManager.log("cloverRuntime = " + cloverRuntime);
        WorkspaceManager.log("reportsDir    = " + reportsDir);
        WorkspaceManager.log("eclipseVersion= " + eclipseVersion);

        WorkspaceManager wm = new WorkspaceManager(projectsDir, cloverRuntime);
        WorkspaceManager.log("--- Phase 1: set classpath variable ---");
        wm.setCloverRuntimeVariable();
        WorkspaceManager.log("--- Phase 2: import projects ---");
        wm.importProjects(SKIPPED_PROJECTS.keySet());
        WorkspaceManager.log("--- Phase 3: build all ---");
        wm.buildAll();
        WorkspaceManager.log("--- Phase 4: verify projects ---");

        List<TestResult> results = new ArrayList<>();
        for (IProject project : wm.getProjects()) {
            long start = System.currentTimeMillis();
            WorkspaceManager.log("Verifying project: " + project.getName());
            TestResult r = new TestResult(project.getName());

            WorkspaceManager.log("  BuildVerifier.verify: " + project.getName());
            BuildVerifier.verify(project, r);

            if (!r.hasBuildErrors()) {
                if (hasUnitTests(project)) {
                    WorkspaceManager.log("  TestRunner.run: " + project.getName());
                    TestRunner.run(project, cloverRuntime, null, r);
                    WorkspaceManager.log("  refresh: " + project.getName());
                    wm.refresh(project);
                    WorkspaceManager.log("  CoverageVerifier.verify: " + project.getName());
                    CoverageVerifier.verify(project, r);
                } else {
                    WorkspaceManager.log("  CoverageVerifier.verifyDbOnly: " + project.getName());
                    CoverageVerifier.verifyDbOnly(project, r);
                }
            } else {
                WorkspaceManager.log("  Build errors in " + project.getName() + " — skipping tests");
            }

            r.setDurationMs(System.currentTimeMillis() - start);
            WorkspaceManager.log("Done: " + project.getName() + " (" + r.getDurationMs() + " ms)");
            results.add(r);
        }

        for (Map.Entry<String, String> entry : SKIPPED_PROJECTS.entrySet()) {
            TestResult r = new TestResult(entry.getKey());
            r.skip(entry.getValue());
            results.add(r);
        }

        SurefireReporter.write(results, reportsDir, eclipseVersion);
        printSummary(results);

        boolean anyFailed = results.stream().anyMatch(TestResult::hasFailed);
        return anyFailed ? EXIT_ERROR : EXIT_OK;
    }

    @Override
    public void stop() {
    }

    private static boolean hasUnitTests(IProject project) throws Exception {
        // A project has unit tests if its .classpath contains JUNIT_CONTAINER.
        IResource classpathFile = project.getFile(".classpath");
        if (!classpathFile.exists()) {
            return false;
        }
        File f = classpathFile.getLocation().toFile();
        String content = new String(java.nio.file.Files.readAllBytes(f.toPath()));
        return content.contains("JUNIT_CONTAINER");
    }

    private static void printSummary(List<TestResult> results) {
        long skipped = results.stream().filter(TestResult::isSkipped).count();
        long passed  = results.stream().filter(r -> !r.hasFailed() && !r.isSkipped()).count();
        long failed  = results.stream().filter(TestResult::hasFailed).count();
        WorkspaceManager.log(String.format("Results: %d passed, %d failed, %d skipped out of %d projects",
                passed, failed, skipped, results.size()));
        results.stream()
               .filter(TestResult::hasFailed)
               .forEach(r -> {
                   WorkspaceManager.log("FAILED: " + r.getProjectName());
                   r.getFailures().forEach(msg -> System.out.println("         " + msg));
               });
    }

    private static File arg(String[] args, String flag) {
        String val = argString(args, flag);
        return val != null ? new File(val) : null;
    }

    private static String argString(String[] args, String flag) {
        if (args == null) {
            return null;
        }
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }
}
