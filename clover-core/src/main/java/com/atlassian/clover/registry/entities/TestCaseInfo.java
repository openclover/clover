package com.atlassian.clover.registry.entities;

import clover.com.google.common.collect.Sets;
import com.atlassian.clover.recorder.PerTestRecordingTranscript;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import static clover.com.google.common.collect.Maps.newHashMap;

public class TestCaseInfo implements Serializable {
    static final long serialVersionUID = 0L;

    public static final int DEFAULT_SLICE_ID_OFFSET = 0;

    private transient WeakReference<FullMethodInfo> sourceMethod;
    private transient WeakReference<FullClassInfo> runtimeType;
    private transient Integer id;

    private String runtimeTypeName;
    private String sourceMethodName;
    private boolean hasResult = false;
    private long startTime;
    private long endTime;
    private double duration = 0;
    private boolean error;
    private boolean failure;
    private String failMessage;
    private String failType;
    private String failFullMessage;

    /** Name of the test defined statically (in the source code) */
    private String staticTestName;

    /** Name of the test defined dynamically (at runtime, by the test framework) */
    @Nullable
    private String runtimeTestName;

    private Integer hashCode = null;

    private transient StackTraceInfo stackTrace;

    public static class Factory {
        private static Map<String, TestCaseInfo> instanceCache = newHashMap();

        private static int sliceOffset = DEFAULT_SLICE_ID_OFFSET;

        public static void reset() {
            instanceCache.clear();
            sliceOffset = DEFAULT_SLICE_ID_OFFSET;
        }

        public static TestCaseInfo getInstance(TestCaseInfo tci) {
            TestCaseInfo result = instanceCache.get(tci.getKey());
            if (result == null) {
                result = tci;
                tci.setId(sliceOffset++);
                instanceCache.put(tci.getKey(), tci);
            }
            return result;
        }

        public static TestCaseInfo getInstanceForSlice(PerTestRecordingTranscript recording) {
            TestCaseInfo tci = new TestCaseInfo(recording.getStart(), recording.getEnd(), recording.getDuration(),
                    recording.getTestTypeName(), recording.getTestMethodName(), recording.getRuntimeTestName());
            TestCaseInfo result = instanceCache.get(tci.getKey());
            if (result == null) {
                result = tci;
                String stackTrace = recording.getStackTrace();
                String exitMessage = recording.getExitMessage();
                tci.setHasResult(recording.hasResult());
                if (recording.hasResult()) {
                    tci.setError(!recording.isResultPassed() && stackTrace == null);
                    tci.setFailure(!recording.isResultPassed() && stackTrace != null);
                    tci.setFailMessage(exitMessage);
                    tci.setFailFullMessage(stackTrace);
                }
                tci.setId(sliceOffset++);
                instanceCache.put(tci.getKey(), tci);
            }
            return result;
        }

        public static Set<TestCaseInfo> getCacheValues() {
            return Sets.newHashSet(instanceCache.values());
        }
    }

    /**
     *
     * @param startTime  approximate time when test has started (in miliseconds since epoch), use 0 if unknown
     * @param endTime    approximate time when test has finished (in miliseconds since epoch), use 0 if unknown
     * @param duration   how long test was running (in seconds)
     * @param runtimeTypeName
     * @param sourceMethodName
     * @param runtimeTestName
     */
    private TestCaseInfo(long startTime, long endTime, double duration,
                         String runtimeTypeName, String sourceMethodName, @Nullable String runtimeTestName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.runtimeTypeName = runtimeTypeName;
        this.sourceMethodName = sourceMethodName;
        this.runtimeType = new WeakReference<FullClassInfo>(null);
        this.sourceMethod = new WeakReference<FullMethodInfo>(null);
        this.runtimeTestName = runtimeTestName;
    }

    public TestCaseInfo(Integer id, FullClassInfo runtimeType, FullMethodInfo sourceMethod,
                        @Nullable String runtimeTestName) {
        this.id = id;
        this.runtimeType = new WeakReference<FullClassInfo>(runtimeType);
        this.sourceMethod = new WeakReference<FullMethodInfo>(sourceMethod);
        // use statically defined test name (if present) or a method name as a name of the test
        this.staticTestName = sourceMethod != null   // may be null in DecoratedTestCaseInfo
                ? (sourceMethod.getStaticTestName() != null ? sourceMethod.getStaticTestName() : sourceMethod.getSimpleName())
                : null;
        this.runtimeTestName = runtimeTestName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isResolved() {
        return getRuntimeType() != null && getSourceMethod() != null;
    }

    /**
     * "resolve" this instance against the given project. The string representations of the runtime type and the
     *  soruce method are matched to the corresponding ClassInfo and MethodInfo objects in the model, if possible.
     * @param project the project to resolve against
     * @return true if resolution was successful
     */
    public boolean resolve(FullProjectInfo project) {
        final String rtClassname = runtimeTypeName.replaceAll("\\.[0-9]+", ""); // hack - see CCD-294, CCD-307
        final FullClassInfo runtimeType = (FullClassInfo)project.findClass(rtClassname);
        this.runtimeType = new WeakReference<FullClassInfo>(runtimeType);
        int lastDot = sourceMethodName.lastIndexOf(".");

        if (lastDot > 0 && lastDot < sourceMethodName.length()) {
            // find a class from a fully qualified test method name
            final String srcClassname = sourceMethodName.substring(0, lastDot);
            final FullClassInfo srcClass = (rtClassname.equals(srcClassname))
                    ? runtimeType : (FullClassInfo)project.findClass(srcClassname);
            FullMethodInfo testMethodFound = null;

            // if found then find proper method in this class as well
            if (srcClass != null) {
                testMethodFound = srcClass.getTestMethodDeclaration(sourceMethodName.substring(lastDot + 1));
                sourceMethod = new WeakReference<FullMethodInfo>(testMethodFound);
            }

            // read static test name out of the method or just guess the name from the substring
            staticTestName = (testMethodFound != null && testMethodFound.getStaticTestName() != null)
                    ? testMethodFound.getStaticTestName()
                    : sourceMethodName.substring(lastDot + 1);

            if (runtimeType != null) {    // - TODO - assess the need to annotate the model like this
                runtimeType.addTestCase(this);
            }

            if (stackTrace != null) {
                stackTrace.resolve(project);
            }
        }

        return isResolved();
    }

    public Object readResolve() throws ObjectStreamException {
        TestCaseInfo info = Factory.getInstance(this);
        StackTraceInfo strace = info.getStackTrace();
        if (strace != null) {
            // make sure the correct parent reference is used
           strace.setOriginatingTest(info); 
        }
        return info;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (failFullMessage != null) {
            stackTrace = new StackTraceInfo(this, failFullMessage);
        }
    }


    public String getKey() {
        return makeKey(runtimeTypeName, sourceMethodName, runtimeTestName ,startTime);
    }

    public static String makeKey(String runtimeTypeName, String sourceMethodName, String runtimeTestName, long startTime) {
        return runtimeTestName == null
                ? runtimeTypeName + "/" + sourceMethodName  + "/" + startTime
                : runtimeTypeName + "/" + sourceMethodName + "/" + runtimeTestName + "/" + startTime;
    }

    public String getClassName() {
        final FullClassInfo runtimeType = this.runtimeType.get();
        return runtimeType == null ? null : runtimeType.getName();
    }

    /**
     * Returns name of the test. Test name resolution is performed in a following order:
     *
     * <ul>
     *  <li>1st - a runtime test name, i.e. the dynamically generated name of the test by test frameworkl;
     *   examples are: Spock @Unroll annotation, JUnit4 @Parameterized annotation</li>
     *  <li>2nd - a statically defined test name, i.e. name declared in the source code, for instance by using annnotations;
     *  examples are: Spock @FeatureMetatdata(name="...")</li>
     *  <li>3rd - a name is taken from name of the method</li>
     * </ul>
     *
     * @return String name of the test
     */
    public String getTestName() {
        return runtimeTestName != null ? runtimeTestName : staticTestName;
    }

    /**
     * Return duration of the test in seconds
     * @return float test duration
     */
    public double getDuration() {
        return duration;
    }

    public boolean isError() {
        return hasResult && error;
    }

    public boolean isHasResult() {
        return hasResult;
    }

    public void setHasResult(boolean hasResult) {
        this.hasResult = hasResult;
    }

    /**
     * Manually set how long test was executing. Method is useful for parsing test results from external sources,
     * like JUnit XML files.
     * @param duration
     */
    public void setDuration(double duration) {
        this.duration = duration;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public boolean isFailure() {
        return hasResult && failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    public boolean isSuccess() {
        return hasResult && !isFailure() && !isError();
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    public String getFailType() {
        return failType;
    }

    public void setFailType(String failType) {
        this.failType = failType;
    }

    public String getFailFullMessage() {
        return failFullMessage;
    }

    public void setFailFullMessage(String failFullMessage) {
        this.failFullMessage = failFullMessage;
        if (failFullMessage != null) {
            stackTrace = new StackTraceInfo(this, failFullMessage);
        }
    }

    @Nullable
    public StackTraceInfo getStackTrace() {
        return stackTrace;
    }

    @Nullable
    public FullClassInfo getRuntimeType() {
        return runtimeType == null ? null : runtimeType.get();
    }

    @Nullable
    public FullMethodInfo getSourceMethod() {
        return sourceMethod == null ? null : sourceMethod.get();
    }

    @Nullable
    public String getQualifiedName() {
        final FullClassInfo runtimeType = getRuntimeType();
        return runtimeType == null ? null : runtimeType.getQualifiedName() + "." + getTestName();
    }

    @Nullable
    public String getRuntimeTypeName() {
        return runtimeTypeName;
    }

    @Nullable
    public String getSourceMethodName() {
        return sourceMethodName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestCaseInfo that = (TestCaseInfo) o;

        if (endTime != that.endTime) return false;
        if (error != that.error) return false;
        if (failure != that.failure) return false;
        if (hasResult != that.hasResult) return false;
        if (startTime != that.startTime) return false;
        if (failFullMessage != null ? !failFullMessage.equals(that.failFullMessage) : that.failFullMessage != null)
            return false;
        if (failMessage != null ? !failMessage.equals(that.failMessage) : that.failMessage != null) return false;
        if (failType != null ? !failType.equals(that.failType) : that.failType != null) return false;
        if (runtimeTypeName != null ? !runtimeTypeName.equals(that.runtimeTypeName) : that.runtimeTypeName != null)
            return false;
        if (sourceMethodName != null ? !sourceMethodName.equals(that.sourceMethodName) : that.sourceMethodName != null)
            return false;
        if (staticTestName != null ? !staticTestName.equals(that.staticTestName) : that.staticTestName != null) return false;
        if (runtimeTestName != null ? !runtimeTestName.equals(that.runtimeTestName) : that.runtimeTestName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            int result;
            result = (runtimeTypeName != null ? runtimeTypeName.hashCode() : 0);
            result = 31 * result + (sourceMethodName != null ? sourceMethodName.hashCode() : 0);
            result = 31 * result + (hasResult ? 1 : 0);
            result = 31 * result + (int) (startTime ^ (startTime >>> 32));
            result = 31 * result + (int) (endTime ^ (endTime >>> 32));
            result = 31 * result + (error ? 1 : 0);
            result = 31 * result + (failure ? 1 : 0);
            result = 31 * result + (failMessage != null ? failMessage.hashCode() : 0);
            result = 31 * result + (failType != null ? failType.hashCode() : 0);
            result = 31 * result + (failFullMessage != null ? failFullMessage.hashCode() : 0);
            result = 31 * result + (staticTestName != null ? staticTestName.hashCode() : 0);
            result = 31 * result + (runtimeTestName != null ? runtimeTestName.hashCode() : 0);
            hashCode = result;
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return "TestCaseInfo[" +
            "id=" + id +
            ", runtimeTypeName='" + runtimeType + '\'' +
            ", staticTestName='" + staticTestName + '\'' +
            ", runtimeTestName='" + runtimeTestName + '\'' +
            ", sourceMethodName='" + sourceMethodName + '\'' +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            ", pass=" + !failure +
            ']';
    }
}
