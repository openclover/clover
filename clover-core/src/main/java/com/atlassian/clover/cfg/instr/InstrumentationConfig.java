package com.atlassian.clover.cfg.instr;

import com.atlassian.clover.instr.tests.DefaultTestDetector;
import com.atlassian.clover.instr.tests.TestDetector;
import org.openclover.runtime.api.CloverException;
import com_atlassian_clover.CloverProfile;
import org.openclover.runtime.Logger;
import org.openclover.runtime.remote.DistributedConfig;
import com_atlassian_clover.CloverVersionInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static org.openclover.util.Lists.newLinkedList;

/**
 * Instrumentation settings common for Java and Groovy.
 */
public class InstrumentationConfig implements Serializable {
    public static final int DIRECTED_FLUSHING = 0;  //todo - remove these and use the ones defined in CoverageRecorder
    public static final int INTERVAL_FLUSHING = 1;
    public static final int THREADED_FLUSHING = 2;

    public static final String DIRECTED_FLUSH_POLICY = "directed";
    public static final String INTERVAL_FLUSH_POLICY = "interval";
    public static final String THREADED_FLUSH_POLICY = "threaded";

    public static final String [] FLUSH_VALUES = {DIRECTED_FLUSH_POLICY, INTERVAL_FLUSH_POLICY, THREADED_FLUSH_POLICY};

    public static final int DEFAULT_FLUSHING = DIRECTED_FLUSHING;

    public static final String DEFAULT_DB_DIR = ".clover";
    public static final String DEFAULT_DB_FILE = "clover"+ CloverVersionInfo.RELEASE_NUM.replace('.','_')+".db";

    private boolean enabled = true;

    /** flushpolicy to use during coverage recording **/
    private int flushPolicy = DEFAULT_FLUSHING;

    /** whether or not to write slice recordings out **/
    private boolean sliceRecording = true;

    /** the minimum interval, in milliseconds, between flushes for interval based flushing **/
    private int flushInterval = 0;

    /** true if we should use a static inner class for instrumentation **/
    private boolean classInstrStrategy = true;

    /** true if errors loading the clover runtime should be reported to the console **/
    private boolean reportInitErrors = true;

    /** if true, tests are rewritten using a synthetic method to record the results **/
    private boolean recordTestResults = true;

    /** whether instrumentation is at the method level or statement level - statement level by default */
    private int instrLevel = InstrumentationLevel.STATEMENT.ordinal();

    /** The files to instrument **/
    private Collection<File> includedFiles;

    private String initString;
    private String projectName = "";
    private File registryFile;
    private boolean relative;
    private String encoding;
    private File defaultBaseDir;
    private TestDetector testDetector;
    private File tmpDir;
    private List<MethodContextDef> methodContexts;
    private List<StatementContextDef> statementContexts;
    /** List of runtime profiles */
    private List<CloverProfile> runtimeProfiles;
    private DistributedConfig distributedConfig;
    private String classNotFoundMsg;
    private String validationFailureReason;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReportInitErrors() {
        return reportInitErrors;
    }

    public void setReportInitErrors(boolean reportInitErrors) {
        this.reportInitErrors = reportInitErrors;
    }

    public boolean isTestDetector() {
        return testDetector != null;
    }

    public TestDetector getTestDetector() {
        if (testDetector == null) {
            testDetector = new DefaultTestDetector();
        }
        return testDetector;
    }

    public void setTestDetector(TestDetector detector) {
        testDetector = detector;
    }

    public String getInitString() {
        return initString;
    }

    public boolean isRelative() {
        return relative;
    }

    public void setRelative(boolean relative) {
        this.relative = relative;
    }

    public int getFlushPolicy() {
        return this.flushPolicy;
    }

    public void setFlushPolicy(int flushPolicy) {
        this.flushPolicy = flushPolicy;
    }

    public void setFlushPolicyFromString(String fp) throws CloverException {
        for (int i = 0; i < FLUSH_VALUES.length; i++) {
            if (FLUSH_VALUES[i].equalsIgnoreCase(fp)) {
                flushPolicy = i;
                return;
            }
        }
        throw new CloverException("Unknown flush policy \"" + fp + "\"");
    }

    public String getFlushPolicyString() {
        return FLUSH_VALUES[flushPolicy];
    }

    public boolean isClassInstrStrategy() {
        return classInstrStrategy;
    }

    public void setClassInstrStragegy(boolean useClass) {
        this.classInstrStrategy = useClass;
    }

    public void setInstrStrategy(String value) {
        if (value != null) {
            classInstrStrategy = value.equals("class");
        }
    }

    /**
     * If the registry file is set, then return it, otherwise
     * a new File object will be created using the initString that
     * is set.
     * @return the registry file to use.
     * @throws CloverException if error resolving initstring
     */
    public File getRegistryFile() throws CloverException {
        return registryFile == null ? new File(getRuntimeInitString()) : registryFile;
    }

    public void setRegistryFile(File registryFile) {
        this.registryFile = registryFile;
    }

    public void setInitstring(String initstring) {
        this.initString = initstring;
    }

    public String getRuntimeInitString() throws CloverException {
        if (getInitString() == null) {
            createDefaultInitStringDir();
        }

        if (isRelative()) {
            return getInitString();
        }
        else {
            return resolveInitString();
        }
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        if (projectName != null) {
            this.projectName = projectName;
        } else {
            Logger.getInstance().debug("Attempt to set the project name to null. Defaulting to '" +
                                        this.projectName + "'");
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isIntervalBasedFlushing() {
       return flushPolicy == INTERVAL_FLUSHING || flushPolicy == THREADED_FLUSHING;
    }

    public boolean isSliceRecording() {
        return sliceRecording;
    }

    public void setSliceRecording(boolean sliceRecording) {
        this.sliceRecording = sliceRecording;
    }

    public int getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(int flushInterval) {
        this.flushInterval = flushInterval;
    }

    public File getDefaultBaseDir() {
        return defaultBaseDir == null ? new File("." + File.separator) : defaultBaseDir;
    }

    public void setDefaultBaseDir(File defaultBaseDir) {
        this.defaultBaseDir = defaultBaseDir;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    public int getInstrLevel() {
        return instrLevel;
    }

    public void setInstrLevel(int instrLevel) {
        this.instrLevel = instrLevel;
    }

    public boolean isStatementInstrEnabled() {
        return instrLevel == InstrumentationLevel.STATEMENT.ordinal();
    }

    public void setInstrLevelStrategy(String value) {
        if (InstrumentationLevel.STATEMENT.name().equalsIgnoreCase(value)) {
            setInstrLevel(InstrumentationLevel.STATEMENT.ordinal());
        } else if (InstrumentationLevel.METHOD.name().equalsIgnoreCase(value)) {
            setInstrLevel(InstrumentationLevel.METHOD.ordinal());
        }
    }

    public String resolveInitString() {
        String resolvedInitString = getInitString();

        File initStringFile = new File(getDefaultBaseDir(), resolvedInitString);
        File initParent = initStringFile.getParentFile();
        if (initParent != null && initParent.exists()) {
            resolvedInitString = initStringFile.getAbsolutePath();
        }
        return resolvedInitString;
    }

    public String createDefaultInitStringDir() throws CloverException {

        File cloverDir = new File(getDefaultBaseDir(), DEFAULT_DB_DIR);
        if ((cloverDir.exists() && !cloverDir.isDirectory()) || (!cloverDir.exists() && !cloverDir.mkdirs())) {
            throw new CloverException("Failed to create default Clover database '" + cloverDir + "'.");
        }
        setInitstring((isRelative() ? cloverDir.getName() : cloverDir.getAbsolutePath()) + File.separatorChar + DEFAULT_DB_FILE);
        return getInitString();
    }

    public void setRecordTestResults(boolean recordTestResults) {
        this.recordTestResults = recordTestResults;
    }

    public boolean isRecordTestResults() {
        return recordTestResults;
    }

    public List<MethodContextDef> getMethodContexts() {
        return methodContexts;
    }

    public void addMethodContext(MethodContextDef context) {
        if (methodContexts == null) {
            methodContexts = newLinkedList();
        }
        methodContexts.add(context);
    }

    public List<StatementContextDef> getStatementContexts() {
        return statementContexts;
    }

    public void addStatementContext(StatementContextDef context) {
        if (statementContexts == null) {
            statementContexts = newLinkedList();
        }
        statementContexts.add(context);
    }

    public boolean hasCustomContexts() {
        return getMethodContexts() != null || getStatementContexts() != null;
    }

    public void setDistributedConfig(DistributedConfig distributedConfig) {
        this.distributedConfig = distributedConfig;
    }

    /** @return the distributedConfig as a String, or null if distributedConfig is null. **/
    public String getDistributedConfigString() {
        return this.distributedConfig != null ? this.distributedConfig.toString() : null;
    }

    public String getClassNotFoundMsg() {
        return classNotFoundMsg;
    }

    public void setClassNotFoundMsg(String classNotFoundMsg) {
        this.classNotFoundMsg = classNotFoundMsg;
    }

    public Collection<File> getIncludedFiles() {
        return includedFiles;
    }

    public void setIncludedFiles(Collection<File> includedFiles) {
        this.includedFiles = includedFiles;
    }

    public void saveToFile(File file) throws IOException {
        Logger.getInstance().verbose("Saving instrumentation config to " + file.getAbsolutePath());
        Logger.getInstance().verbose("Files included for instrumentation: " + getIncludedFiles());
        FileOutputStream fos = new FileOutputStream(file);
        try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this);
            oos.flush();
        }
    }

    public static InstrumentationConfig loadFromStream(InputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(stream);
        InstrumentationConfig config = null;
        try {
            config = (InstrumentationConfig) ois.readObject();
        } finally {
            ois.close();
        }
        Logger.getInstance().verbose("Files included for instrumentation: " + config.getIncludedFiles());
        return config;
    }

    /**
     * Returns list of Clover's runtime profiles or <code>null</code> if none was defined.
     * @return List&lt;CloverProfile&gt;
     */
    public List<CloverProfile> getProfiles() {
        return runtimeProfiles;
    }

    public void addProfile(CloverProfile profile) {
        if (runtimeProfiles == null) {
            runtimeProfiles = newLinkedList();
        }
        runtimeProfiles.add(profile);
    }

    /**
     * Implement validation in sub-classes.
     */
    public boolean validate() {
        return true;
    }

    public String getValidationFailureReason() {
        return validationFailureReason;
    }

    protected void setValidationFailureReason(String reason) {
        validationFailureReason = reason;
    }
}
