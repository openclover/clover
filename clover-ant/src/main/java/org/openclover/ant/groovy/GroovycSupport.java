package org.openclover.ant.groovy;

import org.openclover.ant.tasks.AntInstrumentationConfig;
import org.openclover.ant.tasks.CloverSetupTask;
import org.openclover.ant.tasks.TestSourceSet;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import org.openclover.ant.AntInstrUtils;
import org.openclover.runtime.api.CloverException;
import org.openclover.core.cfg.instr.InstrumentationConfig;
import org.openclover.core.instr.java.Instrumenter;
import org.openclover.core.instr.tests.DefaultTestDetector;
import org.openclover.core.instr.tests.FileMappedTestDetector;
import org.openclover.core.instr.tests.NoTestDetector;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.util.FileUtils;
import org.openclover.core.util.ReflectionUtils;
import org_openclover_runtime.CloverVersionInfo;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.util.ClassPathUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;
import static org.openclover.core.util.Sets.newLinkedHashSet;

public class GroovycSupport implements BuildListener {
    private static final Collection<String> COMPILERS = newHashSet(
            "org.codehaus.groovy.ant.Groovyc",                                        // Groovy compiler itself
            "org.codehaus.groovy.grails.compiler.Grailsc",                            // compiler wrapper since Grails 2.0
            "org.codehaus.groovy.grails.compiler.GrailsCompiler",                     // compiler wrapper for Grails 1.x
            "org.codehaus.groovy.grails.test.compiler.GrailsTestCompiler",            // compiler wrapper since Grails 2.0
            "org.codehaus.groovy.grails.test.compiler.GrailsIntegrationTestCompiler"  // compiler wrapper since Grails 2.3
    );

    private static final String GROVER = "grover";
    private static final String JAR = ".jar";
    private static final String EMBEDDED_GROVER_JAR = String.format("embeddedjars/clover-groovy-%s.jar",
            CloverVersionInfo.RELEASE_NUM);

    private final boolean cleanupAfterBuild;
    private File workingDir;
    private Collection<String> groovycTaskNames = newHashSet();
    private int numTaskDefsLastSeen = 0;
    private File groverJar;

    @SuppressWarnings("unchecked")
    public static void ensureAddedTo(@NotNull final Project project) {
        for (BuildListener listener : project.getBuildListeners()) {
            if (isOneOfMe(listener)) {
                return;
            }
        }
        project.addBuildListener(new GroovycSupport(project));
    }

    private static boolean isOneOfMe(@NotNull final BuildListener listener) {
        // Checks class name in case, somehow, GroovycSupport was loaded by two classloaders
        return listener instanceof GroovycSupport || listener.getClass().getName().equals(GroovycSupport.class.getName());
    }

    private GroovycSupport(@NotNull final Project project) {
        this.cleanupAfterBuild =
                !Boolean.getBoolean(CloverNames.PROP_GROVER_NO_POSTBUILD_CLEANUP)
                        || !Boolean.parseBoolean(project.getProperty(CloverNames.PROP_GROVER_NO_POSTBUILD_CLEANUP));
    }

    @Override
    public void buildStarted(@NotNull final BuildEvent buildEvent) {
    }

    @Override
    public void buildFinished(@NotNull final BuildEvent buildEvent) {
    }

    @Override
    public void targetStarted(@NotNull final BuildEvent buildEvent) {
        logGroovycTasks(buildEvent.getProject());
    }

    private void logGroovycTasks(@NotNull final Project project) {
        // ant.taskdef (name: 'groovyc', classname : 'org.codehaus.groovy.grails.compiler.Grailsc')
        // ant.taskdef (name: 'testc', classname:'org.codehaus.groovy.grails.test.compiler.GrailsTestCompiler', classpathref:"grails.test.classpath")
        // ant.taskdef (name: 'itestc', classname:'org.codehaus.groovy.grails.test.compiler.GrailsIntegrationTestCompiler', classpathref:"grails.test.classpath")
        if (Logger.isDebug()) {
            // clone hashmap before iterating to avoid ConcurrentModificationException
            final Hashtable<String, Class<?>> taskDefinitions = (Hashtable<String, Class<?>>) project.getTaskDefinitions().clone();
            final Set<Map.Entry<String, Class<?>>> entries = taskDefinitions.entrySet();
            for (Map.Entry<String, Class<?>> entry : entries) {
                if ("grailsc".equals(entry.getKey()) || "testc".equals(entry.getKey()) || "itestc".equals(entry.getKey())) {
                    Logger.getInstance().debug("Found compiler task " + entry.getKey() + "=" + entry.getValue().toString());
                }
            }
        }
    }

    @Override
    public void targetFinished(@NotNull final BuildEvent buildEvent) {
    }

    @Override
    public void messageLogged(@NotNull final BuildEvent buildEvent) {
    }

    @Override
    public void taskFinished(@NotNull final BuildEvent buildEvent) {
        if (workingDir != null) {
            if (workingDir.exists() && cleanupAfterBuild) {
                Logger.getInstance().verbose("Cleaning up Clover directory " + workingDir.getAbsolutePath());
                FileUtils.deltree(workingDir);
            }
            workingDir = null;
        }
    }

    @Override
    public void taskStarted(@NotNull final BuildEvent buildEvent) {
        logGroovycTasks(buildEvent.getProject());
        initGroovycTaskNames(buildEvent.getProject());

        final Task task = buildEvent.getTask();

        if (isGroovyc(task)) {
            Logger.getInstance().verbose("Found groovyc task named " + task.getTaskName());

            //Bring task construction forward by a fraction so that we can re-program <groovyc/>
            //but only works > Ant 1.6.5
            task.maybeConfigure();

            try {
                final Task groovyc = task instanceof UnknownElement ? (Task) ((UnknownElement) task).getRealThing() : task;
                final AntInstrumentationConfig config = AntInstrumentationConfig.getFrom(buildEvent.getProject());

                if (groovyc != null) {
                    if (config != null) {
                        if (config.isEnabled()) {
                            if (config.isSkipGroverJar()) {
                                Logger.getInstance().warn("Clover is enabled but has skipGroverJar set to true. Groovy code will not be instrumented");
                            } else {
                                Logger.getInstance().verbose("Clover is enabled in config. Reconfiguring groovyc task " +
                                        task.getTaskName());

                                buildTestDetector(config);
                                workingDir = ensureWorkingDirCreated(config);
                                groverJar = ensureGroverJarCreated(config);
                                if (workingDir != null && groverJar != null) {
                                    prepareForGroovyInstrumentation(groovyc, buildEvent.getProject(), config);
                                }
                            }
                        }
                    } else {
                        Logger.getInstance().info("Clover couldn't find its configuration in a project. Did you forget to call <ant> or <antcall> with inheritRefs=true?");
                    }
                } else {
                    Logger.getInstance().verbose("Real Groovyc task is null. Groovy code will not be instrumented");
                }
            } catch (Exception ex) {
                buildEvent.getProject().log("Clover failed to integrate with <" + task.getTaskName() + "/>", ex, 0);
            }
        }
    }

    /**
     * Extract grover.jar so that groovyc compiler will pick Clover's AST transformer. Also instrument Java files and
     * redirect source folders if we have a joint Groovy-Java compilation.
     */
    public void prepareForGroovyInstrumentation(@NotNull final Task groovyc,
                                                @NotNull final Project project,
                                                @NotNull final AntInstrumentationConfig config)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, CloverException, IOException {

        final Path origSrcPath = invokeGetSrcdir(groovyc);
        final File destDir = invokeGetDestdir(groovyc);

        //Grab groovy source  to compile now because once the inclusion/exclusion is changed it's unretrievable
        final Collection<ParentAndChildPath> groovySourceToCompile = scanForOutOfDateSource(project, groovyc, origSrcPath, destDir, "groovy");

        //Joint compilation is a world of hurt
        if (isJointCompilation(groovyc)) {
            Logger.getInstance().verbose("Detected joint Groovy and Java compilation.");
            final Collection<ParentAndChildPath> javaSourceToCompile = scanForOutOfDateSource(project, groovyc, origSrcPath, destDir, "java");

            //If we need to instrument even a single Java file...
            if (filesNeedingInstrumentation(project, origSrcPath, javaSourceToCompile)) {
                //Create instrumentation dirs
                final File mainInstrDir = AntInstrUtils.createInstrDir(config.getTmpDir());
                final File filteredInstrDir = new File(mainInstrDir, CloverNames.CLOVER_RECORDER_PREFIX);
                filteredInstrDir.mkdirs();
                Logger.getInstance().verbose("Outputting instrumented Java source to " + filteredInstrDir.getAbsolutePath());

                instrumentAndIgnoreOriginalJavaSource(groovyc,
                        config,
                        filteredInstrDir,
                        applyCloverFilter(project, groovyc, origSrcPath, true, javaSourceToCompile));

                //Now *explicitly* tell groovyc to include all groovy source it would have otherwise included (Grover will filter from the plugin
                //with respect to what is supplied to config.setIncludedFiles(...)).
                //Explicit includes are required because we've had to fuck around with the includes / excludes already and may have just made
                //the original source invisible... oops
                includeGroovySource(groovyc, groovySourceToCompile);

                //...add the Java instrumentation dir to the current project path
                invokeSetSrcdir(groovyc, new Path(project, mainInstrDir.getAbsolutePath()));
            }
        }

        final Collection<ParentAndChildPath> groovySourceToInstrument = applyCloverFilter(project, groovyc, origSrcPath, false, groovySourceToCompile);
        final Collection<File> includedFiles = toFiles(groovySourceToInstrument);
        logIncludedFiles(includedFiles);

        //IMPORTANT! This must be set for any Groovy source to be instrumented
        config.setIncludedFiles(includedFiles);

        //Add grover.jar, clover.jar and a configuration dir
        augmentCompilationClasspath(project, groovyc, config);
    }

    private void buildTestDetector(@NotNull final AntInstrumentationConfig config) {
        final TestDetector testDetector;

        if (config.getTestSources() != null) {
            boolean useNoTestDetector = true;
            // if all test sources have enabled="false" then return NoTestDetector
            for (TestSourceSet testSourceSet : config.getTestSources()) {
                if (testSourceSet.isEnabled()) {
                    useNoTestDetector = false;
                    break;
                }
            }

            if (useNoTestDetector) {
                // test detector returning always "false"
                testDetector = new NoTestDetector();
            } else {
                // otherwise build detector based on file set and class/method pattern
                final FileMappedTestDetector fileMappedTestDetector = new FileMappedTestDetector();
                for (TestSourceSet testSourceSet : config.getTestSources()) {
                    // call these getters to force directory scan
                    testSourceSet.getIncludedFiles();
                    testSourceSet.getExcludedFiles();
                    fileMappedTestDetector.addTestSourceMatcher(testSourceSet);
                }
                // test detector based of file+class+method matching
                testDetector = fileMappedTestDetector;
            }
        } else {
            // default test detector
            testDetector = new DefaultTestDetector();
        }

        // store in instrumentation config in order to be serialized
        Logger.getInstance().verbose("Using test detector " + testDetector);
        config.setTestDetector(testDetector);
    }

    private void logIncludedFiles(@NotNull final Collection<File> includedFiles) {
        if (includedFiles.size() > 0) {
            Logger.getInstance().verbose("Groovy files to be instrumented:");
            for (File includedFile : includedFiles) {
                Logger.getInstance().verbose(includedFile.getAbsolutePath());
            }
        }
    }

    @NotNull
    private Collection<File> toFiles(@NotNull final Collection<ParentAndChildPath> parentAndChildPaths) {
        final Collection<File> files = newHashSet();
        for (ParentAndChildPath parentAndChildPath : parentAndChildPaths) {
            files.add(parentAndChildPath.toFile());
        }
        return files;
    }

    private void includeGroovySource(@NotNull final Task groovyc,
                                     @NotNull final Collection<ParentAndChildPath> groovySource) {
        final MatchingTask matchingGroovyc = (MatchingTask) groovyc;
        for (ParentAndChildPath parentAndChildPath : groovySource) {
            Logger.getInstance().verbose("Adding explicit include: " + parentAndChildPath.child);
            matchingGroovyc.setIncludes(parentAndChildPath.child);
        }
    }

    private void instrumentAndIgnoreOriginalJavaSource(@NotNull final Task groovyc,
                                                       @NotNull final AntInstrumentationConfig config,
                                                       @NotNull final File instrDir,
                                                       @NotNull final Collection<ParentAndChildPath> toInstrument) throws CloverException {
        final MatchingTask matchingGroovyc = (MatchingTask) groovyc;

        final Instrumenter instrumenter = new Instrumenter(Logger.getInstance(), config);
        instrumenter.startInstrumentation();

        for (final ParentAndChildPath parentAndChildPath : toInstrument) {
            instrumenter.instrument(parentAndChildPath.toFile(), instrDir, config.getEncoding());
        }

        final String instrumentedSrcIncludes = instrDir.getName() + "/**/*.java";
        Logger.getInstance().verbose("Adding explicit include " + instrumentedSrcIncludes);
        matchingGroovyc.setIncludes(instrumentedSrcIncludes);
        instrumenter.endInstrumentation();
    }

    /**
     * Extracts the grover.jar file from a resource embedded in the Clover JAR file.
     *
     * @param groverJar    target location for extracted grover.jar file or <i>null</i> for default location
     * @param deleteOnExit whether file should be deleted on JVM termination
     * @return File location of extracted grover.jar of <i>null</i> if failed
     */
    @Nullable
    public static File extractGroverJar(@Nullable final File groverJar, boolean deleteOnExit) {
        File jar = null;
        Throwable whyFailed = null;
        final String groverResourceName = "/" + CloverNames.getRepkgPrefix() + EMBEDDED_GROVER_JAR;

        try {

            final InputStream groverStream = CloverSetupTask.class.getResourceAsStream(groverResourceName);
            if (groverStream != null) {
                if (groverJar != null) {
                    // create parent directory if necessary, check if really exists
                    groverJar.getParentFile().mkdirs();
                    if (!groverJar.getParentFile().isDirectory()) {
                        throw new IOException("Directory '" + groverJar.getParentFile().getAbsolutePath() + "' does not exist or couldn't be created");
                    }
                }

                // either use target file name configured by user or create temporary file
                final File jarFile = (groverJar != null ? groverJar : File.createTempFile(GROVER, JAR));
                if (deleteOnExit) {
                    jarFile.deleteOnExit();
                }

                // copy from resource stream to target location
                final OutputStream jarOutStream = Files.newOutputStream(jarFile.toPath());
                final byte[] buffer = new byte[1000];
                int read = groverStream.read(buffer);
                while (read != -1) {
                    jarOutStream.write(buffer, 0, read);
                    read = groverStream.read(buffer);
                }
                jarOutStream.flush();
                jarOutStream.close();
                Logger.getInstance().verbose("Extracted " + groverResourceName + " to " + jarFile.getAbsolutePath());
                jar = jarFile;
            }
        } catch (Throwable t) {
            whyFailed = t;
        }
        if (jar == null) {
            Logger.getInstance().warn("Failed to extract and copy " + groverResourceName + " to a temporary file. "
                    + "Clover instrumentation of Groovy source will not proceed.", whyFailed);
        }
        return jar;
    }

    @Nullable
    private File ensureWorkingDirCreated(@NotNull final AntInstrumentationConfig config) {
        try {
            return FileUtils.createTempDir("grover", config.getTmpDir());
        } catch (Exception e) {
            Logger.getInstance().warn("Failed to create Clover working directory in " + config.getTmpDir() + ". Clover instrumentation of Groovy source will not proceed.", e);
            return null;
        }
    }

    @Nullable
    private File ensureGroverJarCreated(@NotNull final AntInstrumentationConfig config) {
        return extractGroverJar(config.getGroverJar(), cleanupAfterBuild);
    }

    @NotNull
    private Collection<ParentAndChildPath> scanForOutOfDateSource(@NotNull final Project project,
                                                                  @NotNull final Task groovyc,
                                                                  @NotNull final Path origSrcDirs,
                                                                  @Nullable final File destDir,
                                                                  @NotNull final String extension) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Collection<ParentAndChildPath> toBeCompiled = newLinkedHashSet();
        for (String origSrcDirPath : origSrcDirs.list()) {
            File origSrcDir = project.resolveFile(origSrcDirPath);
            if (origSrcDir.exists()) {
                Logger.getInstance().verbose("Scanning for source to compile: " + origSrcDir.getAbsolutePath());
                toBeCompiled.addAll(
                        findOutOfDateSource(
                                groovyc, origSrcDir,
                                destDir != null ? destDir : origSrcDir,
                                invokeGetDirectoryScanner(groovyc, origSrcDir).getIncludedFiles(),
                                extension));
            }
        }

        if (toBeCompiled.size() > 0) {
            Logger.getInstance().verbose("Out of date source found:");
            for (ParentAndChildPath parentAndChildPath : toBeCompiled) {
                Logger.getInstance().verbose(parentAndChildPath.toFile().getAbsolutePath());
            }
        } else {
            Logger.getInstance().verbose("No out of date source found");
        }
        return toBeCompiled;
    }


    private boolean filesNeedingInstrumentation(@NotNull final Project project,
                                                final Path srcPath,
                                                @NotNull final Collection<ParentAndChildPath> sourceToCompile) {
        final Map<File, ParentAndChildPath> toCompileMap = mapToFiles(sourceToCompile);
        final Collection<File> toCompileFiles = newHashSet(toCompileMap.keySet());
        final Collection<File> filteredOut = newLinkedHashSet();
        final Collection<File> toInstrument = newLinkedHashSet();

        AntInstrUtils.sieveSourceForInstrumentation(
                project,
                srcPath,
                AntInstrUtils.calcInstrPatternSet(project),
                AntInstrUtils.calcInstrFileSets(project),
                toCompileFiles,
                filteredOut,
                toInstrument);

        return toInstrument.size() > 0;
    }

    private Collection<ParentAndChildPath> applyCloverFilter(@NotNull final Project project,
                                                             @NotNull final Task groovyc,
                                                             Path srcPath,
                                                             boolean commitFilterToGroovyc,
                                                             @NotNull final Collection<ParentAndChildPath> javaSourceToCompile) {
        Map<File, ParentAndChildPath> toCompileMap = mapToFiles(javaSourceToCompile);
        Collection<File> toCompileFiles = newHashSet(toCompileMap.keySet());

        Collection<File> filteredOut = newLinkedHashSet();
        Collection<File> toInstrument = newLinkedHashSet();
        AntInstrUtils.sieveSourceForInstrumentation(
                project,
                srcPath,
                AntInstrUtils.calcInstrPatternSet(project),
                AntInstrUtils.calcInstrFileSets(project),
                toCompileFiles,
                filteredOut,
                toInstrument);

        if (commitFilterToGroovyc) {
            MatchingTask matchingGroovyc = (MatchingTask) groovyc;
            if (filteredOut.size() > 0) {
                Logger.getInstance().verbose("Adding explicit include(s) for Java source not being instrumented:");
                for (File file : filteredOut) {
                    final ParentAndChildPath parentAndChildPath = toCompileMap.get(file);
                    if (parentAndChildPath != null) {
                        Logger.getInstance().verbose(parentAndChildPath.child);
                        matchingGroovyc.setIncludes(parentAndChildPath.child);
                    }
                }
            }
            if (toInstrument.size() > 0) {
                Logger.getInstance().verbose("Adding explicit exclude(s) for Java source being instrumented:");
                for (File file : toInstrument) {
                    final ParentAndChildPath parentAndChildPath = toCompileMap.get(file);
                    if (parentAndChildPath != null) {
                        Logger.getInstance().verbose(parentAndChildPath.child);
                        matchingGroovyc.setExcludes(parentAndChildPath.child);
                    }
                }
            }
        }


        return grabFromFiles(toCompileMap, toInstrument);
    }

    private Collection<ParentAndChildPath> grabFromFiles(Map<File, ParentAndChildPath> toCompileMap, Collection<File> toInstrument) {
        Collection<ParentAndChildPath> result = newHashSet();
        for (File file : toInstrument) {
            result.add(toCompileMap.get(file));
        }
        return result;
    }

    private Map<File, ParentAndChildPath> mapToFiles(Collection<ParentAndChildPath> toCompile) {
        Map<File, ParentAndChildPath> map = newHashMap();
        for (ParentAndChildPath parentAndChildPath : toCompile) {
            map.put(parentAndChildPath.toFile(), parentAndChildPath);
        }
        return map;
    }

    private void augmentCompilationClasspath(Project project, Task groovyc, AntInstrumentationConfig config) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
        String[] paths = {newConfigDir(config, workingDir).getAbsolutePath(), groverJar.getAbsolutePath(), ClassPathUtil.getCloverJarPath()};
        for (String path : paths) {
            Logger.getInstance().verbose("Adding " + path + " to Groovy compilation path");
            invokeSetClasspath(groovyc, new Path(project, path));
        }
    }

    public static File newConfigDir(InstrumentationConfig config, File parent) throws IOException {
        final File configDir = FileUtils.createTempDir("groverconfig", parent);
        final File instrPropsFile = new File(configDir, CloverNames.getGroverConfigFileName());
        config.saveToFile(instrPropsFile);
        return configDir;
    }

    private Collection<ParentAndChildPath> findOutOfDateSource(@NotNull final Task groovyc,
                                                               @NotNull final File srcDir,
                                                               @NotNull final File destDir,
                                                               @NotNull final String[] files,
                                                               @NotNull final String extension) {
        Collection<ParentAndChildPath> outOfDateFiles = newLinkedHashSet();
        GlobPatternMapper mapper = new GlobPatternMapper();
        SourceFileScanner sfs = new SourceFileScanner(groovyc);
        mapper.setFrom("*." + extension);
        mapper.setTo("*.class");
        final String[] filteredFiles = sfs.restrict(files, srcDir, destDir, mapper);
        if (filteredFiles.length > 0) {
            for (String path : filteredFiles) {
                outOfDateFiles.add(new ParentAndChildPath(srcDir, path));
            }
        }
        return outOfDateFiles;
    }

    private boolean isJointCompilation(@NotNull final Task groovyc) {
        final Enumeration kids = groovyc.getRuntimeConfigurableWrapper().getChildren();
        while (kids.hasMoreElements()) {
            RuntimeConfigurable child = (RuntimeConfigurable) kids.nextElement();
            if (child.getElementTag().equals("javac")) {
                return true;
            }
        }
        return false;
    }

    private DirectoryScanner invokeGetDirectoryScanner(Object task, File file) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return ReflectionUtils.invokeVirtualImplicit("getDirectoryScanner", task, file);
    }

    private Path invokeGetSrcdir(Object task) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return ReflectionUtils.invokeVirtualImplicit("getSrcdir", task);
    }

    private File invokeGetDestdir(Object task) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return ReflectionUtils.invokeVirtualImplicit("getDestdir", task);
    }

    private void invokeSetSrcdir(Object task, Path path) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ReflectionUtils.invokeVirtualImplicit("setSrcdir", task, path);
    }

    private void invokeSetClasspath(Object task, Path path) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ReflectionUtils.invokeVirtualImplicit("setClasspath", task, path);
    }

    @SuppressWarnings("unchecked")
    protected void initGroovycTaskNames(@NotNull final Project project) {
        final Hashtable taskDefs = project.getTaskDefinitions();
        if (taskDefs.size() > numTaskDefsLastSeen) {
            groovycTaskNames = newHashSet();
            for (Map.Entry entry : (Set<Map.Entry>) taskDefs.entrySet()) {
                final Object value = entry.getValue();
                if (value instanceof Class && COMPILERS.contains(((Class) value).getName())) {
                    groovycTaskNames.add((String) entry.getKey());
                    Logger.getInstance().info("Detected groovyc compiler " + entry.getKey() + "=" + ((Class) value).getCanonicalName());
                }
            }
            numTaskDefsLastSeen = taskDefs.size();
        }
    }

    protected boolean isGroovyc(@NotNull final Task task) {
        return groovycTaskNames.contains(task.getTaskName());
    }

    private static class ParentAndChildPath {
        public final File parent;
        public final String child;

        public ParentAndChildPath(File parent, String child) {
            this.parent = parent;
            this.child = child;
        }

        public File toFile() {
            return new File(parent, child);
        }
    }
}
