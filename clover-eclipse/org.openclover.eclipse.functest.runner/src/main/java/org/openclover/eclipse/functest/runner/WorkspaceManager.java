package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WorkspaceManager {

    /** Projects that must be imported before others, in order. */
    private static final List<String> PRIORITY_ORDER = Arrays.asList(
            "TestAllUnitTestsExternal",
            "TestAllUnitTests",
            "TestDependenciesA",
            "TestDependenciesB",
            "TestDependenciesC"
    );

    private final File projectsDir;
    private final String cloverRuntime;
    private final List<IProject> projects = new ArrayList<>();

    public WorkspaceManager(File projectsDir, String cloverRuntime) {
        this.projectsDir = projectsDir;
        this.cloverRuntime = cloverRuntime;
    }

    public void setCloverRuntimeVariable() throws Exception {
        log("Setting CLOVER_RUNTIME classpath variable to: " + cloverRuntime);
        JavaCore.setClasspathVariable("CLOVER_RUNTIME", new Path(cloverRuntime), new NullProgressMonitor());
        log("Waiting for auto-build after CLOVER_RUNTIME variable set...");
        Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        log("Auto-build after CLOVER_RUNTIME variable set: done");
    }

    public void importProjects(Set<String> skipProjects) throws Exception {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        File[] dirs = projectsDir.listFiles(File::isDirectory);
        if (dirs == null) {
            throw new IllegalArgumentException("Not a directory: " + projectsDir);
        }

        // Build an ordered list: priority projects first, then the rest alphabetically.
        LinkedHashSet<String> ordered = new LinkedHashSet<>(PRIORITY_ORDER);
        Arrays.stream(dirs)
              .map(File::getName)
              .filter(name -> !name.startsWith("."))
              .sorted()
              .forEach(ordered::add);

        for (String name : ordered) {
            if (skipProjects.contains(name)) {
                log("Skipping (Tier 3): " + name);
                continue;
            }
            File dir = new File(projectsDir, name);
            if (!dir.isDirectory()) {
                continue;
            }
            File projectFile = new File(dir, ".project");
            if (!projectFile.exists()) {
                log("Skipping " + name + " (no .project file)");
                continue;
            }
            IProjectDescription desc = workspace.loadProjectDescription(
                    new Path(projectFile.getAbsolutePath()));
            desc.setLocation(new Path(dir.getAbsolutePath()));
            IProject project = workspace.getRoot().getProject(desc.getName());
            if (!project.exists()) {
                log("Creating project: " + name);
                project.create(desc, new NullProgressMonitor());
            }
            log("Opening project: " + name);
            project.open(new NullProgressMonitor());
            projects.add(project);
            log("Imported: " + project.getName());
        }
    }

    public void buildAll() throws Exception {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        log("Starting full workspace build...");
        workspace.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
        log("workspace.build() returned; waiting for FAMILY_MANUAL_BUILD jobs...");
        Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
        log("FAMILY_MANUAL_BUILD done; waiting for FAMILY_AUTO_BUILD jobs...");
        Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        log("Build complete");
    }

    public void refresh(IProject project) throws Exception {
        project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
    }

    public List<IProject> getProjects() {
        return projects;
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    static void log(String msg) {
        System.out.println("[runner " + LocalTime.now().format(TIME_FMT) + "] " + msg);
        System.out.flush();
    }
}
