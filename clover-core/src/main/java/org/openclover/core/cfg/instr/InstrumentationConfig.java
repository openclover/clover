package org.openclover.core.cfg.instr;

import org.openclover.core.instr.tests.AggregateTestDetector;
import org.openclover.core.instr.tests.DefaultTestDetector;
import org.openclover.core.instr.tests.FileMappedTestDetector;
import org.openclover.core.instr.tests.NoTestDetector;
import org.openclover.core.instr.tests.SimpleTestSourceMatcher;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.instr.tests.TestDetectorIO;
import org.openclover.core.instr.tests.TestSpec;
import org.openclover.core.io.tags.ObjectReader;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedIO;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.io.tags.Tags;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.remote.DistributedConfig;
import org_openclover_runtime.CloverProfile;
import org_openclover_runtime.CloverVersionInfo;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Sets.newHashSet;

/**
 * Instrumentation settings common for Java and Groovy.
 */
public class InstrumentationConfig implements TaggedPersistent {
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

    /**
     * On-disk format version. Bump whenever the persisted field layout changes so
     * that configs written by an incompatible version are rejected cleanly rather
     * than mis-decoded. Kept in step with the registry / snapshot format versions.
     */
    private static final int CONFIG_FORMAT_VERSION = 50001;

    /**
     * Whitelist of the types that can appear in a serialized instrumentation config.
     * Only the base config itself and the resolved {@link TestDetector} graph are
     * registered - the Ant/Java config subclasses carry no persisted state of their
     * own and are always read/written as the base {@code InstrumentationConfig}.
     * <p>
     * Tag numbers start at {@code NEXT_TAG + 50} to occupy a distinct range from
     * other tag tables (e.g. {@code InstrSessionSegment.TAGS} at {@code NEXT_TAG + 0}
     * and {@code Snapshot.TAGS} at {@code NEXT_TAG + 100}), so reading a stream with
     * the wrong table fails fast with an {@code UnknownTagException}.
     */
    static final Tags TAGS =
        new Tags()
            .registerTag(InstrumentationConfig.class.getName(), Tags.NEXT_TAG + 50, (ObjectReader<InstrumentationConfig>) InstrumentationConfig::read)
            .registerTag(NoTestDetector.class.getName(), Tags.NEXT_TAG + 51, (ObjectReader<NoTestDetector>) NoTestDetector::read)
            .registerTag(DefaultTestDetector.class.getName(), Tags.NEXT_TAG + 52, (ObjectReader<DefaultTestDetector>) DefaultTestDetector::read)
            .registerTag(TestSpec.class.getName(), Tags.NEXT_TAG + 53, (ObjectReader<TestSpec>) TestSpec::read)
            .registerTag(AggregateTestDetector.class.getName(), Tags.NEXT_TAG + 54, (ObjectReader<AggregateTestDetector>) AggregateTestDetector::read)
            .registerTag(FileMappedTestDetector.class.getName(), Tags.NEXT_TAG + 55, (ObjectReader<FileMappedTestDetector>) FileMappedTestDetector::read)
            .registerTag(SimpleTestSourceMatcher.class.getName(), Tags.NEXT_TAG + 56, (ObjectReader<SimpleTestSourceMatcher>) SimpleTestSourceMatcher::read);

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
            throw new CloverException("Failed to create default OpenClover database '" + cloverDir + "'.");
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
        try (FileChannel channel = FileChannel.open(file.toPath(), WRITE, CREATE, TRUNCATE_EXISTING)) {
            TaggedIO.write(channel, TAGS, InstrumentationConfig.class, this);
        }
    }

    public static InstrumentationConfig loadFromStream(InputStream stream) throws IOException {
        final InstrumentationConfig config =
            TaggedIO.read(new DataInputStream(stream), TAGS, InstrumentationConfig.class);
        Logger.getInstance().verbose("Files included for instrumentation: " + config.getIncludedFiles());
        return config;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeInt(CONFIG_FORMAT_VERSION);

        out.writeBoolean(enabled);
        out.writeInt(flushPolicy);
        out.writeBoolean(sliceRecording);
        out.writeInt(flushInterval);
        out.writeBoolean(classInstrStrategy);
        out.writeBoolean(reportInitErrors);
        out.writeBoolean(recordTestResults);
        out.writeInt(instrLevel);
        out.writeBoolean(relative);

        out.writeUTF(initString);
        out.writeUTF(projectName);
        out.writeUTF(encoding);
        out.writeUTF(classNotFoundMsg);
        out.writeUTF(validationFailureReason);

        writeFile(out, registryFile);
        writeFile(out, defaultBaseDir);
        writeFile(out, tmpDir);

        // includedFiles (nullable collection)
        out.writeBoolean(includedFiles != null);
        if (includedFiles != null) {
            out.writeInt(includedFiles.size());
            for (File file : includedFiles) {
                out.writeUTF(file.getPath());
            }
        }

        // testDetector - resolved detector graph, via the whitelist
        TestDetectorIO.writeDetector(out, testDetector);

        // distributedConfig - persisted as its round-trippable config string
        out.writeUTF(distributedConfig == null ? null : distributedConfig.getConfigString());

        writeContextDefs(out, methodContexts, statementContexts);
        writeProfiles(out, runtimeProfiles);
    }

    public static InstrumentationConfig read(TaggedDataInput in) throws IOException {
        final int formatVersion = in.readInt();
        if (formatVersion != CONFIG_FORMAT_VERSION) {
            throw new IOException("Unsupported instrumentation config format version " + formatVersion
                + " (expected " + CONFIG_FORMAT_VERSION + ")");
        }

        final InstrumentationConfig config = new InstrumentationConfig();
        config.enabled = in.readBoolean();
        config.flushPolicy = in.readInt();
        config.sliceRecording = in.readBoolean();
        config.flushInterval = in.readInt();
        config.classInstrStrategy = in.readBoolean();
        config.reportInitErrors = in.readBoolean();
        config.recordTestResults = in.readBoolean();
        config.instrLevel = in.readInt();
        config.relative = in.readBoolean();

        config.initString = in.readUTF();
        config.projectName = in.readUTF();
        config.encoding = in.readUTF();
        config.classNotFoundMsg = in.readUTF();
        config.validationFailureReason = in.readUTF();

        config.registryFile = readFile(in);
        config.defaultBaseDir = readFile(in);
        config.tmpDir = readFile(in);

        if (in.readBoolean()) {
            final int count = in.readInt();
            final Collection<File> files = newHashSet();
            for (int i = 0; i < count; i++) {
                files.add(new File(in.readUTF()));
            }
            config.includedFiles = files;
        }

        config.testDetector = in.read(TestDetector.class);

        final String distributedConfigString = in.readUTF();
        config.distributedConfig = distributedConfigString == null ? null : new DistributedConfig(distributedConfigString);

        readContextDefs(in, config);
        readProfiles(in, config);

        return config;
    }

    private static void writeFile(TaggedDataOutput out, File file) throws IOException {
        out.writeUTF(file == null ? null : file.getPath());
    }

    private static File readFile(TaggedDataInput in) throws IOException {
        final String path = in.readUTF();
        return path == null ? null : new File(path);
    }

    private static void writeContextDefs(TaggedDataOutput out, List<MethodContextDef> methodContexts,
                                         List<StatementContextDef> statementContexts) throws IOException {
        out.writeBoolean(methodContexts != null);
        if (methodContexts != null) {
            out.writeInt(methodContexts.size());
            for (MethodContextDef def : methodContexts) {
                out.writeUTF(def.getName());
                out.writeUTF(def.getRegexp());
                out.writeInt(def.getMaxComplexity());
                out.writeInt(def.getMaxStatements());
                out.writeInt(def.getMaxAggregatedComplexity());
                out.writeInt(def.getMaxAggregatedStatements());
            }
        }

        out.writeBoolean(statementContexts != null);
        if (statementContexts != null) {
            out.writeInt(statementContexts.size());
            for (StatementContextDef def : statementContexts) {
                out.writeUTF(def.getName());
                out.writeUTF(def.getRegexp());
            }
        }
    }

    private static void readContextDefs(TaggedDataInput in, InstrumentationConfig config) throws IOException {
        if (in.readBoolean()) {
            final int count = in.readInt();
            for (int i = 0; i < count; i++) {
                final String name = in.readUTF();
                final String regexp = in.readUTF();
                final MethodContextDef def = new MethodContextDef();
                def.setName(name);
                def.setRegexp(regexp);
                def.setMaxComplexity(in.readInt());
                def.setMaxStatements(in.readInt());
                def.setMaxAggregatedComplexity(in.readInt());
                def.setMaxAggregatedStatements(in.readInt());
                config.addMethodContext(def);
            }
        }

        if (in.readBoolean()) {
            final int count = in.readInt();
            for (int i = 0; i < count; i++) {
                final StatementContextDef def = new StatementContextDef();
                def.setName(in.readUTF());
                def.setRegexp(in.readUTF());
                config.addStatementContext(def);
            }
        }
    }

    private static void writeProfiles(TaggedDataOutput out, List<CloverProfile> profiles) throws IOException {
        out.writeBoolean(profiles != null);
        if (profiles != null) {
            out.writeInt(profiles.size());
            for (CloverProfile profile : profiles) {
                out.writeUTF(profile.getName());
                out.writeUTF(profile.getCoverageRecorder().name());
                final DistributedConfig distributed = profile.getDistributedCoverage();
                out.writeUTF(distributed == null ? null : distributed.getConfigString());
            }
        }
    }

    private static void readProfiles(TaggedDataInput in, InstrumentationConfig config) throws IOException {
        if (in.readBoolean()) {
            final int count = in.readInt();
            for (int i = 0; i < count; i++) {
                final String name = in.readUTF();
                final String coverageRecorder = in.readUTF();
                final String distributedCoverage = in.readUTF();
                config.addProfile(new CloverProfile(name, coverageRecorder, distributedCoverage));
            }
        }
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
