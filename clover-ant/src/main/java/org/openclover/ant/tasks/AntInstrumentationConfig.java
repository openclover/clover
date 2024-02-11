package org.openclover.ant.tasks;

import org.openclover.runtime.CloverNames;
import org.openclover.runtime.api.CloverException;
import org.openclover.core.cfg.instr.InstrumentationConfig;
import org.openclover.core.cfg.instr.InstrumentationPlacement;
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig;
import org.openclover.core.cfg.instr.InstrumentationLevel;
import org.openclover.core.cfg.instr.java.LambdaInstrumentation;
import org.openclover.core.util.ArrayUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Sets.newHashSet;

public class AntInstrumentationConfig extends JavaInstrumentationConfig {
    private final transient Project project;

    protected transient List<FileSet> instrFilesets = null;
    protected transient List<TestSourceSet> testSources = null;
    private transient PatternSet instrPattern;

    private boolean preserve;
    private String compilerDelegate;
    private File groverJar;
    private boolean skipGroverJar = false;

    public AntInstrumentationConfig(Project project) {
        super();
        this.project = project;
        setProjectName(project.getName());
        setDefaultBaseDir(project.getBaseDir());
        setInitstring(project.getProperty(CloverNames.PROP_INITSTRING));
    }

    /**
     * Overridden method that resolves the init string against the project's basedir.
     *
     * @return resolved initstring
     */
    @Override
    public String resolveInitString() {

        if (getInitString() == null) { // first check the attribute on this class

            // next check for a project config object with a pre-resolved initstring
            AntInstrumentationConfig cfg = project.getReference(CloverNames.PROP_CONFIG);
            String initString = null;

            if (cfg != null) {
                initString = cfg.getInitString();
            }

            if (initString == null) {
                // next check for a project property that is set
                initString = project.getProperty(CloverNames.PROP_INITSTRING);
                if (initString == null) {
                    try {
                        createDefaultInitStringDir(); // finally, just set the default location
                    } catch (CloverException e) {
                        throw new BuildException(e.getMessage() + " Please use the \"initstring\" attribute to specify a Clover database location.");
                    }
                } else {
                    setInitstring(initString);
                }
            } else {
                setInitstring(initString);
            }
        }

        String resolvedInitString = getInitString();
        if (project != null) {
            File initStringFile = project.resolveFile(getInitString());
            File initParent = initStringFile.getParentFile();
            if (initParent != null && initParent.exists()) {
                resolvedInitString = initStringFile.getAbsolutePath();
            }
        }
        return resolvedInitString;
    }


    public boolean isPreserve() {
        return preserve;
    }

    public void setPreserve(boolean preserve) {
        this.preserve = preserve;
    }

    public String getCompilerDelegate() {
        return compilerDelegate;
    }

    public void setCompilerDelegate(String compilerDelegate) {
        this.compilerDelegate = compilerDelegate;
    }

    @Nullable
    public static AntInstrumentationConfig getFrom(@NotNull final Project project) {
        return project.getReference(CloverNames.PROP_CONFIG);
    }

    public void setIn(Project project) {
        project.addReference(CloverNames.PROP_CONFIG, this);
    }

    public void addConfiguredFileSet(FileSet set) {
        if (this.instrFilesets == null) {
            this.instrFilesets = newArrayList();
        }
        this.instrFilesets.add(set);
    }

    public void addConfiguredTestSources(TestSourceSet testSourceSet) {
        if (this.testSources == null) {
            this.testSources = newArrayList();
        }
        testSourceSet.validate();
        this.testSources.add(testSourceSet);
    }

    public List<FileSet> getInstrFilesets() {
        return instrFilesets;
    }

    public List<TestSourceSet> getTestSources() {
        return testSources;
    }

    /** Extra setter so that we can call it from Grails' BuildConfig */
    public void setInstrumentLambda(String instrumentLambda) {
        super.setInstrumentLambda(LambdaInstrumentation.valueOf(instrumentLambda.toUpperCase(Locale.ENGLISH)));
    }

    public void setInstrPattern(PatternSet filesPattern) {
        this.instrPattern = filesPattern;
    }

    public PatternSet getInstrPattern() {
        return instrPattern;
    }

    public void setGroverJar(File groverJar) {
        this.groverJar = groverJar;
    }

    public File getGroverJar() {
        return groverJar;
    }

    public void setSkipGroverJar(boolean skip) {
        this.skipGroverJar = skip;
    }

    public boolean isSkipGroverJar() {
        return skipGroverJar;
    }

    /**
     * Ant Enumerated Attribute subclass for the clover flush policy
     */
    public static class FlushPolicy extends EnumeratedAttribute {
        /**
         * Standard EnumeratedAttribute method to get the list of allowed values
         *
         * @return an array of allowed values.
         */
        @Override
        public String[] getValues() {
            return InstrumentationConfig.FLUSH_VALUES;
        }
    }

    /**
     * Ant Enumerated Attribute subclass for the clover flush policy
     */
    public static class Instrumentation extends EnumeratedAttribute {

        /**
         * Standard EnumeratedAttribute method to get the list of allowed values
         *
         * @return an array of allowed values.
         */
        @Override
        public String[] getValues() {
            return ArrayUtil.toLowerCaseStringArray(InstrumentationPlacement.values());
        }
    }

    /**
     * Ant enumerated attribute subclass for the Clover instrumentation level policy
     */
    public static class EnumInstrumentationLevel extends EnumeratedAttribute {
        @Override
        public String[] getValues() {
            InstrumentationLevel[] levels = InstrumentationLevel.values();
            String[] values = new String[levels.length];
            for (int i = 0; i < levels.length; i++) {
                values[i] = levels[i].name().toLowerCase();
            }
            return values;
        }
    }

    public void configureIncludedFiles() {
        if (instrFilesets != null) {
            Collection<File> includedFiles = newHashSet();
            for (FileSet fileset : instrFilesets) {
                final String[] included = fileset.getDirectoryScanner(project).getIncludedFiles();
                for (String path : included) {
                    includedFiles.add(new File(fileset.getDir(project), path));
                }
            }
            setIncludedFiles(includedFiles);
        }
    }

}
