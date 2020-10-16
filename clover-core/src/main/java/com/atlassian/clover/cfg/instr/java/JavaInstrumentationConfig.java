package com.atlassian.clover.cfg.instr.java;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.util.JavaEnvUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Supplementary instrumentation settings related with Java.
 */
public class JavaInstrumentationConfig extends InstrumentationConfig {
    public static final String JAVA_LANG_PREFIX = "java.lang.";

    private SourceLevel sourceLevel = SourceLevel.autoDetect();

    /** if true, use fully qualified names for Java vars **/
    private boolean fullyQualifiedJavaNames = true;

    private String instrFileExtension = "java";

    private LambdaInstrumentation instrumentLambda = LambdaInstrumentation.NONE;

    /** Used by CloverInstr */
    private File sourceDir;

    /** Used by CloverInstr */
    private File destDir;

    /** Used by CloverInstr */
    private List<String> sourceFiles = new ArrayList<>();

    public String getJavaLangPrefix() {
        return fullyQualifiedJavaNames ? JAVA_LANG_PREFIX : "";
    }

    public void setFullyQualifyJavaLang(boolean fullyQualifiedJavaNames) {
        this.fullyQualifiedJavaNames = fullyQualifiedJavaNames;
    }

    public SourceLevel getSourceLevel() {
        return sourceLevel;
    }

    /**
     * Java language level of sources being instrumented.
     */
    public void setSourceLevel(SourceLevel sourceLevel) {
        this.sourceLevel = sourceLevel;
    }

    public void setInstrFileExtension(String extension) {
        instrFileExtension = extension;
    }

    public String getInstrFileExtension() {
        return instrFileExtension;
    }

    public void setInstrumentLambda(LambdaInstrumentation instrumentLambda) {
        this.instrumentLambda = instrumentLambda;
    }

    public LambdaInstrumentation getInstrumentLambda() {
        return instrumentLambda;
    }

    public void setSourceDir(File sourceDir) {
        this.sourceDir = sourceDir;
    }

    public File getSourceDir() {
        return sourceDir;
    }

    public File getDestDir() {
        return destDir;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void addSourceFile(String fileName) {
        sourceFiles.add(fileName);
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }

    @Override
    public boolean validate() {
        // ensure we have the location of the instrumenation database
        if (getInitString() == null) {
            try {
                createDefaultInitStringDir();
            } catch (CloverException e) {
                setValidationFailureReason("No initstring value supplied, and default location could not be created: " + e.getMessage());
                return false;
            }
        }

        // ensure we have anything to instrument
        if (sourceDir == null && sourceFiles.size() == 0) {
            setValidationFailureReason("Neither source directory nor source files are specified");
            return false;
        }

        // ensure we have target directory
        if (destDir == null) {
            setValidationFailureReason("No destination directory specified");
            return false;
        }

        // ensure target directory and it's not the same as or inside the source directory
        if (sourceDir != null) {
            if (sourceDir.equals(destDir)) {
                setValidationFailureReason("Source and destination directories cannot be the same.");
                return false;
            }

            // check to see that indir is not a parent of outdir
            File destDirParent = destDir.getParentFile();
            while (destDirParent != null) {
                if (destDirParent.equals(sourceDir)) {
                    setValidationFailureReason("Cannot specify a destination directory that is a nested dir of the source directory.");
                    return false;
                }
                destDirParent = destDirParent.getParentFile();
            }
        }

        // don't set flush interval for directed policy
        if (getFlushPolicy() == InstrumentationConfig.DIRECTED_FLUSHING && getFlushInterval() != 0) {
            setValidationFailureReason("Flush policy is 'directed', so you cannot specify a flush interval. To specify an interval change the flush policy to interval or threaded.");
            return false;
        }

        // require interval for interval or threaded policy
        if ((getFlushPolicy() == InstrumentationConfig.INTERVAL_FLUSHING ||
                getFlushPolicy() == InstrumentationConfig.THREADED_FLUSHING) && getFlushInterval() == 0) {
            setValidationFailureReason("When using either 'interval' or 'threaded' flush policy, a flush interval must be specified.");
            return false;
        }

        // all ok
        return true;
    }
}
