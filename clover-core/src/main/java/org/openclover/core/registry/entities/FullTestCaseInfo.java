package org.openclover.core.registry.entities;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.recorder.PerTestRecordingTranscript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;

public class FullTestCaseInfo implements TestCaseInfo, Serializable {
    static final long serialVersionUID = 0L;

    public static final int DEFAULT_SLICE_ID_OFFSET = 0;

    private transient WeakReference<MethodInfo> sourceMethod;
    private transient WeakReference<ClassInfo> runtimeType;
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
        private static Map<String, FullTestCaseInfo> instanceCache = newHashMap();

        private static int sliceOffset = DEFAULT_SLICE_ID_OFFSET;

        public static void reset() {
            instanceCache.clear();
            sliceOffset = DEFAULT_SLICE_ID_OFFSET;
        }

        public static FullTestCaseInfo getInstance(FullTestCaseInfo tci) {
            FullTestCaseInfo result = instanceCache.get(tci.getKey());
            if (result == null) {
                result = tci;
                tci.setId(sliceOffset++);
                instanceCache.put(tci.getKey(), tci);
            }
            return result;
        }

        public static FullTestCaseInfo getInstanceForSlice(PerTestRecordingTranscript recording) {
            FullTestCaseInfo tci = new FullTestCaseInfo(recording.getStart(), recording.getEnd(), recording.getDuration(),
                    recording.getTestTypeName(), recording.getTestMethodName(), recording.getRuntimeTestName());
            FullTestCaseInfo result = instanceCache.get(tci.getKey());
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

        public static Set<FullTestCaseInfo> getCacheValues() {
            return newHashSet(instanceCache.values());
        }
    }

    /**
     * @param startTime  approximate time when test has started (in miliseconds since epoch), use 0 if unknown
     * @param endTime    approximate time when test has finished (in miliseconds since epoch), use 0 if unknown
     * @param duration   how long test was running (in seconds)
     */
    private FullTestCaseInfo(long startTime, long endTime, double duration,
                             String runtimeTypeName, String sourceMethodName, @Nullable String runtimeTestName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.runtimeTypeName = runtimeTypeName;
        this.sourceMethodName = sourceMethodName;
        this.runtimeType = new WeakReference<>(null);
        this.sourceMethod = new WeakReference<>(null);
        this.runtimeTestName = runtimeTestName;
    }

    public FullTestCaseInfo(Integer id, ClassInfo runtimeType, MethodInfo sourceMethod,
                            @Nullable String runtimeTestName) {
        this.id = id;
        this.runtimeType = new WeakReference<>(runtimeType);
        this.sourceMethod = new WeakReference<>(sourceMethod);
        // use statically defined test name (if present) or a method name as a name of the test
        this.staticTestName = sourceMethod != null   // may be null in DecoratedTestCaseInfo
                ? (sourceMethod.getStaticTestName() != null ? sourceMethod.getStaticTestName() : sourceMethod.getSimpleName())
                : null;
        this.runtimeTestName = runtimeTestName;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean isResolved() {
        return getRuntimeType() != null && getSourceMethod() != null;
    }

    /**
     * "resolve" this instance against the given project. The string representations of the runtime type and the
     *  soruce method are matched to the corresponding ClassInfo and MethodInfo objects in the model, if possible.
     * @param project the project to resolve against
     * @return true if resolution was successful
     */
    @Override
    public boolean resolve(ProjectInfo project) {
        final String rtClassname = runtimeTypeName.replaceAll("\\.[0-9]+", ""); // hack - see CCD-294, CCD-307
        final FullClassInfo runtimeType = (FullClassInfo)project.findClass(rtClassname);
        this.runtimeType = new WeakReference<>(runtimeType);
        int lastDot = sourceMethodName.lastIndexOf(".");

        if (lastDot > 0 && lastDot < sourceMethodName.length()) {
            // find a class from a fully qualified test method name
            final String srcClassname = sourceMethodName.substring(0, lastDot);
            final FullClassInfo srcClass = (rtClassname.equals(srcClassname))
                    ? runtimeType : (FullClassInfo)project.findClass(srcClassname);
            MethodInfo testMethodFound = null;

            // if found then find proper method in this class as well
            if (srcClass != null) {
                testMethodFound = srcClass.getTestMethodDeclaration(sourceMethodName.substring(lastDot + 1));
                sourceMethod = new WeakReference<>(testMethodFound);
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
        FullTestCaseInfo info = Factory.getInstance(this);
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

    @Override
    public String getKey() {
        return makeKey(runtimeTypeName, sourceMethodName, runtimeTestName ,startTime);
    }

    public static String makeKey(String runtimeTypeName, String sourceMethodName, String runtimeTestName, long startTime) {
        return runtimeTestName == null
                ? runtimeTypeName + "/" + sourceMethodName  + "/" + startTime
                : runtimeTypeName + "/" + sourceMethodName + "/" + runtimeTestName + "/" + startTime;
    }

    @Override
    public String getClassName() {
        final ClassInfo runtimeType = this.runtimeType.get();
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
    @Override
    public String getTestName() {
        return runtimeTestName != null ? runtimeTestName : staticTestName;
    }

    /**
     * Return duration of the test in seconds
     * @return float test duration
     */
    @Override
    public double getDuration() {
        return duration;
    }

    @Override
    public boolean isError() {
        return hasResult && error;
    }

    @Override
    public boolean isHasResult() {
        return hasResult;
    }

    public void setHasResult(boolean hasResult) {
        this.hasResult = hasResult;
    }

    /**
     * Manually set how long test was executing. Method is useful for parsing test results from external sources,
     * like JUnit XML files.
     */
    public void setDuration(double duration) {
        this.duration = duration;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    @Override
    public boolean isFailure() {
        return hasResult && failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    @Override
    public boolean isSuccess() {
        return hasResult && !isFailure() && !isError();
    }

    @Override
    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    @Override
    public String getFailType() {
        return failType;
    }

    public void setFailType(String failType) {
        this.failType = failType;
    }

    @Override
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
    @Override
    public StackTraceInfo getStackTrace() {
        return stackTrace;
    }

    @Nullable
    @Override
    public ClassInfo getRuntimeType() {
        return runtimeType == null ? null : runtimeType.get();
    }

    @Nullable
    @Override
    public MethodInfo getSourceMethod() {
        return sourceMethod == null ? null : sourceMethod.get();
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        final ClassInfo runtimeType = getRuntimeType();
        return runtimeType == null ? null : runtimeType.getQualifiedName() + "." + getTestName();
    }

    @Nullable
    @Override
    public String getRuntimeTypeName() {
        return runtimeTypeName;
    }

    @Nullable
    @Override
    public String getSourceMethodName() {
        return sourceMethodName;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
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

        FullTestCaseInfo that = (FullTestCaseInfo) o;

        if (endTime != that.endTime) return false;
        if (error != that.error) return false;
        if (failure != that.failure) return false;
        if (hasResult != that.hasResult) return false;
        if (startTime != that.startTime) return false;
        if (!Objects.equals(failFullMessage, that.failFullMessage))
            return false;
        if (!Objects.equals(failMessage, that.failMessage)) return false;
        if (!Objects.equals(failType, that.failType)) return false;
        if (!Objects.equals(runtimeTypeName, that.runtimeTypeName))
            return false;
        if (!Objects.equals(sourceMethodName, that.sourceMethodName))
            return false;
        if (!Objects.equals(staticTestName, that.staticTestName)) return false;
        return Objects.equals(runtimeTestName, that.runtimeTestName);
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
