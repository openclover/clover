package org.openclover.ant.taskdefs;

import clover.org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.jetbrains.annotations.Nullable;
import org.openclover.ant.AntInstrUtils;
import org.openclover.ant.AntLogger;
import org.openclover.ant.tasks.AntInstrumentationConfig;
import org.openclover.ant.tasks.TestSourceSet;
import org.openclover.core.Contract;
import org.openclover.core.cfg.instr.java.SourceLevel;
import org.openclover.core.instr.java.Instrumenter;
import org.openclover.core.instr.tests.FileMappedTestDetector;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.util.ClassPathUtil;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.openclover.core.util.Sets.newHashSet;

/**
 * error handling in this class sux - but it is forced on us by Ant, or more
 * precisely Javac, which doesn't respect Build exceptions thrown out of
 * setJavac(). So we have to store them and throw them later out of execute.
 */
public class CloverCompilerAdapter implements CompilerAdapter {
    /**
     * A helper class to "fiddle" with the "build.complier" of
     * compilerVersion setting in the associated Javac instance. <p>
     *
     * This fixes bug Defect-6486: The problem is that if you pass
     * "source=1.3" to the JAVAC task in Ant, and you are running under JDK
     * 1.3, then the following happens: a) In Ant1.4.1, "-source" get's
     * passed to javac.exe, which it doesn't understand; b) In Ant1.5+, some
     * smarts occur, and "-source" WILL NOT get passed to javac.exe; c) In
     * Ant1.5+, but when "with.clover" is enabled, then the "smarts" do not
     * kick in, java.exe complains that it does not understand "-source".
     * <p/>
     * The problem c) occurs because the "smarts" depends on the setting of
     * the compilerVersion (aka build.compiler) property. <p>
     * <p/>
     * The solution, which this class implements, is to "fudge" then
     * "restore" the compilerVersion setting before calling the underlying
     * compiler adapther
     */
    class CompilerVersionFiddler {
        private String ourCompilerStr = null;


        /**
         * set the compilerVersion to something the underlying
         * CompilerAdapter will understand.
         */
        void push() {
            try {
                ourCompilerStr = javac.getCompilerVersion();
                // the underlying compiler needs to think it is the compiler for this javac
                javac.setCompiler(compilerDelegate);
            } catch (NoSuchMethodError e) {
                // ignore, nothing we can do about this bug in Ant141 anyway
            }
        }


        /** restore compilerVersion  */
        void pop() {
            if (ourCompilerStr != null) {
                try {
                    javac.setCompiler(ourCompilerStr);// restore it
                } catch (NoSuchMethodError e) {
                    // ignore, nothing we can do about this bug in Ant141 anyway
                }
            }
            ourCompilerStr = null;
        }

    }


    /** use to log messages * */
    private Logger log;
    private String compilerDelegate;

    private Javac javac;
    private CompilerAdapter underlyingCompiler;
    private Project project;
    private CloverException error;
    private String javacName;
    private File tmpDir;
    private boolean preserveInstr;

    private CompilerVersionFiddler compilerVersionFiddler = new CompilerVersionFiddler();

    private void cloverLogging() {
        javacName = javac.getTaskName();
        javac.setTaskName("clover");
    }

    private void javacLogging() {
        javac.setTaskName(javacName);
    }

    /**
     * @param aJavac  The new javac value
     * @see CompilerAdapter#setJavac *
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setJavac(final Javac aJavac) {
        Contract.pre(aJavac != null);

        javac = aJavac;
        project = javac.getProject();

        Logger.setInstance(new AntLogger(project, javac));
        log = Logger.getInstance();
        cloverLogging();

        if (javac.getDestdir() == null) {
            error = new CloverException("When using OpenClover you need to set the destdir on <javac>");
            return;
        }

        AntInstrumentationConfig instrConfig = AntInstrumentationConfig.getFrom(project);
        if (instrConfig == null) {
            log.warn("OpenClover configuration was not found (did you call <ant> or <antcall> without inheritRefs=true?)."
                    + " Default settings for code instrumentation will be used.");
            instrConfig = new AntInstrumentationConfig(project);
        }

        compilerDelegate = instrConfig.getCompilerDelegate();
        preserveInstr = instrConfig.isPreserve();

        if (javac.isForkedJavac()) {
            log.info("Fork is set to true and will be respected (ignore any warnings from Ant).");
            if (compilerDelegate != null && !"extJavac".equals(compilerDelegate)) {
                log.warn("Ignoring clover.compiler setting because fork is true. Using 'extJavac' compiler.");
            }
            compilerDelegate = "extJavac";
        }

        if (compilerDelegate != null && compilerDelegate.equals(this.getClass().getName())) {
            error = new CloverException("You cannot set clover.compiler to be the OpenClover Compiler Adapter. Check the value you are passing to clover.compiler");
            return;
        }

        if (compilerDelegate == null) {
            compilerDelegate = getDefaultAntCompiler();
        }

        log.debug("using underlying compiler '" + compilerDelegate + "'");
        underlyingCompiler = CompilerAdapterFactory.getCompiler(compilerDelegate, javac);
        log.debug("underlying compiler instance is " + underlyingCompiler.getClass().getName());


        if (instrConfig.isEnabled()) {

            // locally set source level overrides previously configured value
            final String src = javac.getSource();
            if (src != null) {
                instrConfig.setSourceLevel(SourceLevel.fromString(src));
            }
 
            // locally set encoding overrides previously configured value
            final String encoding = javac.getEncoding();
            if (encoding != null) {
                instrConfig.setEncoding(javac.getEncoding());
            }

            // determine if there are any patternsets restricting instrumentation
            final PatternSet instrPatternSet = AntInstrUtils.calcInstrPatternSet(project);

            // determine if there are any filesets restricting instrumentation
            final List<FileSet> instrFileSetsList = AntInstrUtils.calcInstrFileSets(project);

            // the compileset has the original list of files that were handed to javac to compile
            final File[] compileList = getJavacCompileList();
            if (compileList == null) {
                error = new CloverException("The javac.compileList is null. Unable to integrate OpenClover with Javac.");
                return;
            }
            final Set<File> compileSet = newHashSet(compileList);

            //the copyset contains files that are to be compiled without instrumentation. Initially
            //this is the entire compile set
            final Collection<File> copySet = newHashSet(compileSet);

            //the instrset contains files that need instrumentation. This is initially empty.
            final Collection<File> instrSet = newHashSet();

            //Sort source into copied source and instrumented source
            AntInstrUtils.sieveSourceForInstrumentation(project, javac.getSrcdir(), instrPatternSet, instrFileSetsList, compileSet, copySet, instrSet);

            // apply test source definitions if they are specified
            final TestDetector testDetector = calcTestDetector(compileSet, copySet, instrSet);
            if (testDetector != null) {
                instrConfig.setTestDetector(testDetector);
            }

            // generate a new list for the compiler. Add all the files that won't be instrumented.
            final Collection<File> replacementCompileSet = newHashSet(copySet);

            try {
                tmpDir = AntInstrUtils.createInstrDir(instrConfig.getTmpDir());

                final Instrumenter instrumenter = new Instrumenter(log, instrConfig);
                // do the actual instrumentation
                instrumenter.startInstrumentation();
                for (final File f : instrSet) {
                    final File copy = instrumenter.instrument(f, tmpDir, instrConfig.getEncoding());
                    replacementCompileSet.add(copy);
                }

                instrumenter.endInstrumentation();
                if ( !setJavacCompileList(replacementCompileSet.toArray(new File[0])) ) {
                    error = new CloverException("Failed to write to javac.compileList field. Unable to integrate OpenClover with Javac.");
                    return;
                }
            } catch (CloverException e) {
                error = e;
                log.error(error.getMessage());
                log.error("** Error(s) occurred and the instrumentation process can't continue.");
                AntInstrUtils.cleanUpInstrDir(tmpDir, preserveInstr);
            }

            final String cloverPathStr = ClassPathUtil.getCloverJarPath();
            if (cloverPathStr != null) {
                final Path cloverPath = new Path(project, cloverPathStr);
                javac.setClasspath(cloverPath);
            }
        } else {
            log.info("** OpenClover is disabled. Delegating straight to the compiler.");
        }

        compilerVersionFiddler.push();
        underlyingCompiler.setJavac(javac);
        compilerVersionFiddler.pop();

        javacLogging();
    }

    /**
     * Read value of the super.javac.compileList field, which has non-public visiblity.
     * @return javac.compileList protected field or <code>null</code> if reflection failed
     */
    @Nullable
    private File[] getJavacCompileList() {
        try {
            return (File[])FieldUtils.readField(javac, "compileList", true);
        } catch (SecurityException | ClassCastException | IllegalAccessException ex) {
            log.error("** Failed to access javac.compileList protected field", ex);
        }
        return null;
    }

    /**
     * Write value to the super.javac.compileList field, which has non-public visiblity.
     * @return boolean <code>true</code> if field was set, <code>false</code> otherwise
     */
    private boolean setJavacCompileList(File[] newValue) {
        try {
            FieldUtils.writeField(javac, "compileList", newValue, true);
            return true;
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            log.error("** Failed to access javac.compileList protected field", ex);
        }
        return false;
    }

    private TestDetector calcTestDetector(Set<File> compileSet, Collection<File> copySet, Collection<File> instrSet) {
        final AntInstrumentationConfig config = AntInstrumentationConfig.getFrom(project);

        if (config != null && config.getTestSources() != null) {
            final FileMappedTestDetector fileMappedTestDetector = new FileMappedTestDetector();
            final List<TestSourceSet> testSourcesList = config.getTestSources();

            for (final TestSourceSet testSourceSet : testSourcesList) {
                final Set<File> included = testSourceSet.getIncludedFiles();
                for (final File inc : included) {
                    if (compileSet.contains(inc)) {
                        instrSet.add(inc);
                        copySet.remove(inc);
                    }
                }
                final Set<File> excluded = testSourceSet.getExcludedFiles();
                for (final File exc : excluded) {
                    if (compileSet.contains(exc)) {
                        instrSet.remove(exc);
                        copySet.add(exc);
                    }
                }
                fileMappedTestDetector.addTestSourceMatcher(testSourceSet);
            }

            return fileMappedTestDetector;
        }

        return null;
    }

    public static String getDefaultAntCompiler() {
        return "modern";
    }

    /**
     * @see CompilerAdapter#execute() *
     */
    @Override
    public boolean execute() throws BuildException {
        cloverLogging();

        if (error != null) {
            Throwable cause = error;
            if (error.getCause() != null) {
                cause = error.getCause();
            }
            throw new BuildException(cause);
        }
        try {
            log.debug("delegating to compiler impl: " + underlyingCompiler.getClass().getName());
            // delegate to the actual compiler instance
            javacLogging();

            compilerVersionFiddler.push();
            final boolean retval = underlyingCompiler.execute();
            compilerVersionFiddler.pop();

            return retval;
        } finally {
            cloverLogging();
            AntInstrUtils.cleanUpInstrDir(tmpDir, preserveInstr);
            javacLogging();
        }
    }
}

