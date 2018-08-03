package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.ant.groovy.GroovycSupport;
import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import org.openclover.util.ClassPathUtil;
import org.apache.tools.ant.BuildException;
import com.atlassian.clover.ant.taskdefs.CloverCompilerAdapter;
import org.apache.tools.ant.types.PatternSet;

import java.io.File;

/**
 * Clover Task which sets up clover and encapsulates all the
 * complexity of the clover compiler adapter setup (namely the
 * properties required to be setup.
 *
 * @since 1.1
 */
public class CloverSetupTask extends AbstractInstrTask {
    /**
     * Standard Ant property for compiler selection
     */
    public static final String BUILD_COMPILER = "build.compiler";

    /**
     * The Clover compiler adapter class
     */
    public static final String CLOVER_ADAPTER = "com.atlassian.clover.ant.taskdefs.CloverCompilerAdapter";


    /**
     * Set the flag which controls whether clover is enabled
     * or not.
     *
     * @param enabled if true clover will instrument files as they are compiled.
     */
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    /**
     * Set the underlying compiler that clover will use to compile the code.
     *
     * @param cloverCompiler the compiler spec - this is the same as the 
     *        standard Ant <code>build.compiler</code> value.
     */
    public void setCloverCompiler(String cloverCompiler) {
        config.setCompilerDelegate(cloverCompiler);
    }

    /**
     * Flag to indicate whether the instrumented source should be saved.
     *
     * @param preserve if true the instrumented code is kept.
     */
    public void setPreserve(boolean preserve) {
        config.setPreserve(preserve);
    }

    /**
     * Sets a fixed name and location for the temporary grover.jar artifact which is being
     * used for compilation of Groovy code. By default grover*.jar has unique, generated
     * file name and is located in ${java.io.tmpdir} directory. Note that file will be
     * deleted on JVM termination unless "clover.no.postbuild.cleanup" property is defined.
     *
     * @param groverJar path where grover.jar will be written
     * @since 3.1.8
     */
    public void setGroverJar(File groverJar) {
        config.setGroverJar(groverJar);
    }

    /**
     * If set to true, then skips generation of grover.jar artifact during setup. Can be
     * set to true only if there is no Groovy code in the project.
     * @param skip if true then grover.jar will not be created and Groovy code not instrumented
     * @since 3.1.8
     */
    public void setSkipGroverJar(boolean skip) {
        config.setSkipGroverJar(skip);
    }

    /**
     * Set the pattern set for the files to be included or
     * excluded from clovering
     *
     * @param filesPattern the patternset to use
     */
    public void addFiles(PatternSet filesPattern) {
        this.config.setInstrPattern(filesPattern);
    }

    /**
     * Setup Clover to run
     *
     * Note that this task just enables the clover compiler adapter -
     * it does not do much more than setup some properties and structures
     */
    @Override
    public void cloverExecute() {
        try {
            final AntInstrumentationConfig existingCfg = AntInstrumentationConfig.getFrom(getProject());
            if (existingCfg != null) {
                Logger.getInstance().debug("Existing Clover configuration found with initstring \"" + existingCfg.getInitString() + "\". Overriding.");
            }
        }
        catch (ClassCastException e) {
            Logger.getInstance().warn("Clover error: Unknown type for reference " + CloverNames.PROP_CONFIG + ": ", e);
        }

        String existingBuildCompiler = getProject().getProperty(BUILD_COMPILER);

        // Check if the CloverCompilerAdapter is already installed
        if (CLOVER_ADAPTER.equals(existingBuildCompiler)) {
            // if it is, remove it for the moment, and set it to the default
            String cmpDelegate = config.getCompilerDelegate();
            getProject().setProperty(BUILD_COMPILER, cmpDelegate == null ? CloverCompilerAdapter.getDefaultAntCompiler() : cmpDelegate);
        }

        config.setIn(getProject());

        GroovycSupport.ensureAddedTo(getProject());

        if (!config.isEnabled()) {
            getProject().log("Clover is disabled.");
            return;
        }

        // try to resolve the initstring
        String runtimeInitString = getRuntimeInitString();

        Logger.getInstance().info("Clover is enabled with initstring '" + runtimeInitString + "'");

        String buildCompiler = getProject().getProperty(BUILD_COMPILER);

        if (buildCompiler != null && !buildCompiler.equals(CLOVER_ADAPTER) && config.getCompilerDelegate() == null) {
            config.setCompilerDelegate(buildCompiler);
        }

        getProject().setProperty(BUILD_COMPILER, CLOVER_ADAPTER);
        getProject().setProperty(CloverNames.PROP_INITSTRING, runtimeInitString);

        if (config.getCompilerDelegate() != null && config.getCompilerDelegate().equals(CLOVER_ADAPTER)) {
            throw new BuildException("You cannot set clovercompiler to be the Clover compiler adapter. Check the" +
                    " value you are passing to clovercompiler, or the value of the build.compiler Ant property");

        }

        String cloverJarPath = ClassPathUtil.getCloverJarPath();

        if (cloverJarPath != null) {
            getProject().setProperty(CloverNames.PROP_CLOVER_JARPATH, cloverJarPath);
        }
    }
}
