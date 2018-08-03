package com.atlassian.clover.eclipse.core.projects.builder;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.projects.model.CoverageFilesUtils;
import com.atlassian.clover.eclipse.core.projects.model.LoadedDatabaseModel;
import com.atlassian.clover.eclipse.core.projects.model.CoverageModelChangeEvent;
import com.atlassian.clover.eclipse.core.projects.model.DuringFullBuildDatabaseModel;
import com.atlassian.clover.eclipse.core.ui.widgets.MessageDialogWithCheckbox;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.JavaEnvUtils;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.registry.Clover2Registry;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.internal.compiler.batch.CloverCompiler;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Version;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Lists.newLinkedList;

public class BuildCoordinator {
    private static final int TOTAL_COMPILATION_PROGRESS = 1000;
    private static final int INSTRUMENTATION_PROGRESS = 400;
    private static final int COMPILATION_PROGRESS = 400;
    private static final int REFRESHING_OUTPUT_LOCATION_PROGRESS = 100;
    private static final int REMOVING_WORK_AREA_PROGRESS = 100;

    protected final CloverProject project;

    public BuildCoordinator(CloverProject project) {
        this.project = project;
    }

    public void registerFilesForInstrumentation(BuildContext[] files) throws CoreException {
        if (files.length > 0) {
            Set<IFile> beingCompiled = project.getFilesNeedingCloverCompile();
            if (beingCompiled == null) {
                beingCompiled = new LinkedHashSet<IFile>();
                project.setFilesNeedingCloverCompile(beingCompiled);
            }
            for (BuildContext file : files) {
                beingCompiled.add(file.getFile());
            }
        }
    }

    private boolean isPreserveInstrumentedSources() {
        boolean preserve = CloverPlugin.getInstance().getInstallationSettings().isPreserveInstrumentedSources();
        CloverPlugin.logVerbose("Instrumented sources will " + (preserve ? "" : "not" ) + " be preserved in temporary directory.");
        return preserve;
    }

    public void onEndOfBuild(final int buildKind, final IProgressMonitor monitor) throws CoreException {
        final Set dirtyFiles = project.getFilesNeedingCloverCompile();

        monitor.beginTask("Compiling with Clover", TOTAL_COMPILATION_PROGRESS);
        try {
            if (shouldBuild(dirtyFiles)) {
                monitor.subTask("Waiting for Clover model to load");
                CloverDatabase database = project.joinOnLoad(monitor);
                if (database != null) {
                    final Clover2Registry registry;
                    if (shouldBuild(dirtyFiles) && isFullBuild(buildKind)) {
                        project.setModel(new DuringFullBuildDatabaseModel(project, CoverageModelChangeEvent.COMPILE(project)));
                        database = project.newEmptyDatabase();
                    } else {
                        if (database == null) {
                            database = project.newEmptyDatabase();
                        }
                    }
                    registry = database.getRegistry();

                    final File workArea = createCompilationWorkArea();
                    final Path workAreaPath = new Path(workArea.getAbsolutePath());
                    final InstrumentationProjectPathMap workAreaPathMap = new InstrumentationProjectPathMap(project, workAreaPath);

                    final BaseInstrumenter instrumenter =
                        instrument(
                            registry,
                            workAreaPathMap,
                            dirtyFiles,
                            SubMonitor.convert(monitor, INSTRUMENTATION_PROGRESS),
                            buildKind);

                    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                        @Override
                        public void run(IProgressMonitor monitor) throws CoreException {
                            try {
                                compile(
                                    workAreaPathMap,
                                    instrumenter,
                                    SubMonitor.convert(monitor, COMPILATION_PROGRESS));
                            } finally {
                                if (isPreserveInstrumentedSources()) {
                                    File copyArea = createInstrSourcesCopyWorkArea();
                                    try {
                                        FileUtils.dirCopy(workArea, copyArea, false);
                                        CloverPlugin.logInfo("CLOVER: Instrumented sources have been preserved in " + copyArea.getAbsolutePath());
                                    } catch (IOException ex) {
                                        CloverPlugin.logInfo("CLOVER: Failed to copy instrumented sources from "
                                                + workArea.getAbsolutePath() + " to "
                                                + copyArea.getAbsolutePath());
                                    }
                                } else {
                                    CloverPlugin.logDebug("Removing instrumented sources from " + workArea.getAbsolutePath());
                                    removeWorkArea(workArea, monitor);
                                }
                            }
                        }
                    }, monitor);

                    final boolean includeFailedCoverage = CloverPlugin.getInstance().getInstallationSettings().isIncludeFailedCoverage();
                    project.setModel( new LoadedDatabaseModel(
                            project, database, CoverageModelChangeEvent.COMPILE(project), includeFailedCoverage) );
                }
            }
        } finally {
            monitor.done();
            project.clearFilesNeedingCloverCompile();
        }
    }

    private boolean shouldBuild(Set dirtyFiles) {
        return dirtyFiles != null && dirtyFiles.size() > 0;
    }

    private boolean isFullBuild(int buildKind) {
        return buildKind == IncrementalProjectBuilder.FULL_BUILD;
    }

    private Clover2Registry createEmptyRegistry() {
        return new Clover2Registry(project.getRegistryFile(), project.getName());
    }

    private void refreshOutputLocations(IProgressMonitor monitor) throws CoreException {
        monitor.subTask("Refreshing output folders");
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        final IPath[] outputPaths = project.getPathMap().getAllOutputLocations();
        for (IPath outputPath : outputPaths) {
            IResource outputLocation = workspaceRoot.findMember(outputPath);
            if (outputLocation != null && outputLocation instanceof IContainer) {
                outputLocation.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
        }
        if (!project.getSettings().isOutputRootSameAsProject()) {
            IContainer outputContainer =
                PathUtils.containerFor(
                    project.getProject().getFullPath().append(project.getSettings().getOutputRoot()));
            if (outputContainer != null && outputContainer.exists()) {
                outputContainer.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
        }
        monitor.worked(REFRESHING_OUTPUT_LOCATION_PROGRESS);
    }

    private BaseInstrumenter instrument(Clover2Registry registry,
                                        InstrumentationProjectPathMap workAreaPathMap,
                                        Set<IFile> filesNeedingCompile,
                                        IProgressMonitor monitor,
                                        int buildKind) throws CoreException {
        monitor.beginTask("Clover: Instrumenting project source", 10);
        try {
            if (filesNeedingCompile.size() > 0) {
                boolean successful = true;
                CloverPlugin.logVerbose("Starting instrumentation");

                long start = System.currentTimeMillis();

                BaseInstrumenter instrumenter = new FileBasedInstrumenter(project, registry, workAreaPathMap,
                        SubMonitor.convert(monitor, 10), buildKind);
                instrumenter.start(filesNeedingCompile.size());
                instrumentFiles: {
                    for (IFile file : filesNeedingCompile) {
                        try {
                            if (monitor.isCanceled()) {
                                break instrumentFiles;
                            }
                            instrumenter.instrumentOrCopySource(file);
                        } catch (InstrumentationInitialisationException e) {
                            //Let initialisation problems bubble up as there's no point continuing
                            throw e;
                        } catch (InstrumentationException e) {
                            //We don't do a big song and dance here as it could just be
                            //that one of the instrumented source files had a typo
                            //and Eclipse just shows problem markers
                            CloverPlugin.logVerbose("Instrumenting file " + file + " failed, syntax error in original source?", e);
                            successful = false;
                        }
                    }
                }

                if (!monitor.isCanceled()) {
                    instrumenter.finish(successful);
                    CloverPlugin.logVerbose("Ending instrumentation, took " + (System.currentTimeMillis() - start) + "ms");
                    return instrumenter;
                }
            }
            return null;
        } finally {
            monitor.done();
        }
    }

    private File createCompilationWorkArea() throws CoreException {
        try {
            //Eclipse per-project working dir
            File tempFolder = project.getWorkingPath().toFile();
            return FileUtils.createTempDir("CLOV", tempFolder);
        } catch (IOException e) {
            throw CloverPlugin.logAndThrowError("Unable to create working folder", e);
        }
    }

    /**
     * Creates a temporary directory in which copy of instrumented sources will be stored.
     * We have one such folder per project and its name is constant between builds (as opposed
     * to <pre>createCompilationWorkArea()</pre>).
     * @return File - temporary directory
     */
    private File createInstrSourcesCopyWorkArea() {
        File workDir = new File(project.getWorkingPath().toFile(), "CLOV_INSTR_SRC");
        workDir.mkdir();
        return workDir;
    }

    private void compile(InstrumentationProjectPathMap workAreaPathMap, BaseInstrumenter instrumenter, IProgressMonitor monitor) throws CoreException {
        if (instrumenter != null && !monitor.isCanceled()) {
            monitor.subTask("Compiling Clover-instrumented source");
            CloverPlugin.logVerbose("Starting instrumented compilation");
            long start = System.currentTimeMillis();

            try {
                List<String> command = newArrayList();

                addOutputPathToClasspath(project.getJavaProject(), command);
                outputLibrariesToClasspath(
                    command,
                    collectClasspathEntries(
                        /* set of projects used to detect project cycles */
                        new HashSet(),
                        /* oredered set of classpath entries */
                        new LinkedHashSet(),
                        project.getJavaProject(),
                        false));

                addMiscParams(command);
                addTargetParam(command);
                addSourceParam(command);
                addNoWarningsFlag(command);
                addProceedOnErrorFlag(command);
                addEncodingParam(command);
                addSourceFilesToCompile(instrumenter, command);

                CloverPlugin.logVerbose("Running compilation command: " + command);
                final File outFile = File.createTempFile("cloverCompilationLog", ".out");
                outFile.deleteOnExit();
                final PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
                final File errFile = File.createTempFile("cloverCompilationLog", ".err");
                errFile.deleteOnExit();
                final PrintWriter err = new PrintWriter(new BufferedWriter(new FileWriter(errFile)));
                try {
                    if (!new CloverCompiler(out, err, instrumenter, workAreaPathMap, monitor)
                            .compile(command.toArray(new String[command.size()]))) {
                        CloverPlugin.logVerbose("Instrumented compilation returned false");
                        CloverPlugin.logVerbose("Instrumented compilation out: " + outFile.getAbsolutePath());
                        CloverPlugin.logVerbose("Instrumented compilation error: " + errFile.getAbsolutePath());
                    }
                } finally {
                    out.close();
                    err.close();
                }
            } catch (Exception e) {
                CloverPlugin.logError("Error compiling instrumented source", e);
            }
            CloverPlugin.logVerbose("Ending instrumented compilation, took " + (System.currentTimeMillis() - start) + "ms");
        } else {
            CloverPlugin.logVerbose("Not performing instrumented compilation - nothing to do or compilation cancelled");
        }
        monitor.worked(COMPILATION_PROGRESS);
    }

    private void addSourceFilesToCompile(BaseInstrumenter instrumenter, List<String> command) {
        for (Iterator<String> iterator = instrumenter.fileNamesAsCompilerArg(); iterator.hasNext();) {
            command.add(iterator.next());
        }
    }

    private void addEncodingParam(List<String> command) {
        // use the default charset for the encoding
        // all files have been instrumented or copied using this charset
        command.add("-encoding ");
        command.add("UTF-8");
    }

    private void addProceedOnErrorFlag(List<String> command) {
        command.add("-proceedOnError");
    }

    private void addNoWarningsFlag(List<String> command) {
        command.add("-nowarn");
    }

    private void addSourceParam(List<String> command) throws CoreException {
        command.add("-source");
        command.add(project.getJavaProject().getOption(JavaCore.COMPILER_SOURCE, true));
    }

    private void addTargetParam(List<String> command) throws CoreException {
        command.add("-target");
        command.add(
            project.getJavaProject().getOption(
                JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
    }

    private void addMiscParams(List<String> command) throws CoreException {
        List<String> debugOptions = newLinkedList();
        if (JavaCore.GENERATE.equals(
            project.getJavaProject().getOption(
                JavaCore.COMPILER_LINE_NUMBER_ATTR, true))) {
            debugOptions.add("lines");
        }
        if (JavaCore.GENERATE.equals(
            project.getJavaProject().getOption(
                JavaCore.COMPILER_LOCAL_VARIABLE_ATTR, true))) {
            debugOptions.add("vars");
        }
        if (JavaCore.GENERATE.equals(
            project.getJavaProject().getOption(
                JavaCore.COMPILER_SOURCE_FILE_ATTR, true))) {
            debugOptions.add("source");
        }

        StringBuilder debugSwitch = new StringBuilder("-g:");
        if (debugOptions.size() == 0) {
            debugSwitch.append("none");
        } else {
            for (Iterator<String> iterator = debugOptions.iterator(); iterator.hasNext();) {
                debugSwitch.append(iterator.next());
                if (iterator.hasNext()) {
                    debugSwitch.append(",");
                }
            }
        }
        command.add(debugSwitch.toString());

        if (JavaCore.PRESERVE.equals(
            project.getJavaProject().getOption(
                JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL, true))) {
            command.add("-preserveAllLocals");
        }

        if (JavaCore.ENABLED.equals(
            project.getJavaProject().getOption(
                JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, true))) {
            command.add("-inlineJSR");
        }

        addVersionSpecificMiscParams(command);
    }

    private void addVersionSpecificMiscParams(List<String> command) throws CoreException {
        Version version = JDTUtils.getJDTVersion();
        if (version.getMajor() == 3 && version.getMinor() >= 3) {
            //No annotation processing
            command.add("-proc:none");
        }
    }

    private Set collectClasspathEntries(Set projectSet, Set classpathEntries, IJavaProject currentProject, boolean checkExported) throws CoreException {
        IWorkspaceRoot root = currentProject.getProject().getWorkspace().getRoot();
        //Ignore projects cycles if they exist
        if (projectSet.add(currentProject)) {
            IClasspathEntry[] entries = currentProject.getResolvedClasspath(true);
            for (IClasspathEntry entry : entries) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    //Only process exported entries of dependent projects or all entries of the main project
                    if (checkExported && entry.isExported() || !checkExported) {
                        IPath absolutePath = entry.getPath();

                        //Workspace anchored paths e.g. /MyProject/myjar.jar
                        IResource resource = root.findMember(entry.getPath());
                        if (resource == null || !resource.exists()) {
                            //Project anchored paths e.g. /myjar.jar
                            resource = currentProject.getProject().findMember(entry.getPath());
                        }

                        absolutePath = (resource != null && resource.exists()) ? resource.getLocation() : absolutePath;
                        classpathEntries.add(absolutePath.toOSString());
                    }
                } else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    //Recurse dependent projects if Java based
                    IProject dependentProject =
                            ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().lastSegment());
                    IJavaProject dependentJavaProject;
                    if (dependentProject.exists() && dependentProject.isAccessible() && ((dependentJavaProject = JavaCore.create(dependentProject)) != null)) {
                        classpathEntries.addAll(new ProjectPathMap(dependentJavaProject).getOutputLocations());
                        collectClasspathEntries(projectSet, classpathEntries, dependentJavaProject, true);
                    }
                }
            }
        }
        return classpathEntries;
    }

    private void outputLibrariesToClasspath(List<String> command, Set<String> libraries) throws CoreException {
        if (libraries.size() > 0) {
            for (String library : libraries) {
                if (command.size() == 0) {
                    //CEP-162: setting the bootclasspath of the compiler
                    //so that the right JRE classes are used for compilation
                    command.add("-bootclasspath");
                    command.add(library);
                } else {
                    appendToLast(command, File.pathSeparatorChar + library);
                }
            }
        }
    }

    private void addOutputPathToClasspath(ProjectPathMap projectPathMap, List<String> command) throws CoreException {
        //Add output path to classpath for incremental builds
        String outputClasspath = projectPathMap.toClasspath();
        if (outputClasspath.length() > 0) {
            if (command.size() == 0) {
                //CEP-162: setting the bootclasspath of the compiler
                //so that the right JRE classes are used for compilation
                command.add("-bootclasspath");
                command.add(outputClasspath);
            } else {
                appendToLast(command, File.pathSeparatorChar + outputClasspath);
            }
        }
    }

    private void appendToLast(List<String> command, String value) {
        command.set(
            command.size() - 1,
            (command.get(command.size() - 1) + value));
    }

    private void addOutputPathToClasspath(IJavaProject project, List<String> command) throws CoreException {
        addOutputPathToClasspath(new ProjectPathMap(project), command);
    }

    private void removeWorkArea(File lastCompilationDir, IProgressMonitor monitor) {
        CloverPlugin.logDebug("Deleting class files in " + lastCompilationDir);
        lastCompilationDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    file.listFiles(this);
                    file.delete();
                    return false;
                } else {
                    if (file.isFile()) {
                        file.delete();
                    }
                    return true;
                }
            }
        });
        lastCompilationDir.delete();
        monitor.worked(REMOVING_WORK_AREA_PROGRESS);
    }

    public void onClean(IProgressMonitor monitor) {
        final boolean includeFailedCoverage = CloverPlugin.getInstance().getInstallationSettings().isIncludeFailedCoverage();
        project.setModel( new LoadedDatabaseModel(
                project, project.newEmptyDatabase(), CoverageModelChangeEvent.COMPILE(project), includeFailedCoverage) );

        project.setLastCleanBuildStamp(System.currentTimeMillis());

        final boolean shouldClearCoverage;
        if (CloverPlugin.getInstance().getInstallationSettings().isPromptingOnRebuild()) {
            final MessageDialogWithCheckbox.Result result = new MessageDialogWithCheckbox.Result();
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    MessageDialogWithCheckbox.openQuestion(
                        null,
                        "Delete existing Clover coverage data?",
                        "You are doing a rebuild, do you want delete the old coverage data for project \"" + project.getName() + "\"?",
                        true,
                        "Display this prompt again?", true,
                        result);
                }
            });

            shouldClearCoverage = result.isYesSelected();

            CloverPlugin.getInstance().getInstallationSettings().setLatestPromptOnRebuildDecision(result.isYesSelected());
            CloverPlugin.getInstance().getInstallationSettings().setPromptingOnRebuild(result.isChecked());
        } else {
            shouldClearCoverage = CloverPlugin.getInstance().getInstallationSettings().isDeletingCoverageOnRebuild();
        }

        nukeCoverageData(shouldClearCoverage, monitor);
        nukeSeparateInstrumentedClasses(monitor);
    }

    private void nukeSeparateInstrumentedClasses(final IProgressMonitor monitor) {
        try {
            final IFolder instrumentationOutput = project.getInstrumentationOutputRootDir();

            if (instrumentationOutput != null && instrumentationOutput.exists()) {
                project.runOnWorkingDir(
                    new CloverProject.Callable() {
                        @Override
                        public void call() throws CoreException {
                            //Resync with file system so deletes less likely to fail
                            instrumentationOutput.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                            instrumentationOutput.accept(
                                new CleaningVisitor(instrumentationOutput, monitor));
                        }
                    });
            }
        } catch (Exception e) {
            CloverPlugin.logError("Unable to clean Clover output directory", e);
        }
    }

    private void nukeCoverageData(boolean shouldClearCoverage, IProgressMonitor monitor) {
        //First nuke coverage data
        if (shouldClearCoverage) {
            CloverPlugin.logVerbose("Clearing coverage information");
            CoverageFilesUtils.deleteCoverageFiles(project.getRegistryFile(), true, monitor);
        }
    }
}
