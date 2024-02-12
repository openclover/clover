package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.Logger;
import org.openclover.runtime.RuntimeType;
import org.openclover.runtime.util.CloverBitSet;
import org.openclover.runtime.util.IOStreamUtils;
import org_openclover_runtime.Clover;
import org_openclover_runtime.CoverageRecorder;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Per-test coverage recording from the currently running application
 */
public class FileBasedPerTestRecording extends BaseCoverageRecording implements LivePerTestRecording {
    private final CloverBitSet coverage;
    private final String testTypeName;
    private final String testMethodName;
    private final String runtimeTestName;
    private final long start;
    private final double duration;
    private final String exitMessage;
    private final String stackTrace;
    private final int exitStatus;

    public FileBasedPerTestRecording(String dbName, long dbVersion, int recorderId, CloverBitSet coverage,
            String testMethodName, String runtimeTestName,
            long start, long end, double duration, RuntimeType testType,
            int slice, int testRunId, int exitStatus, ErrorInfo errorInfo) {
        super(
            new Header(dbVersion, end, FORMAT),
            new File(Clover.getSliceRecordingName(testType.id, slice, testRunId, recorderId, dbName, start)));
        this.coverage = coverage;
        this.testTypeName = testType.name;
        this.testMethodName = testMethodName;
        this.runtimeTestName = runtimeTestName;
        this.start = start;
        this.duration = duration;
        this.exitStatus = exitStatus;
        this.exitMessage = errorInfo == null ? null : errorInfo.getMessage();
        this.stackTrace = errorInfo == null ? null : errorInfo.getStackTrace();
    }

    public FileBasedPerTestRecording(CoverageRecorder recorder, CloverBitSet coverage,
            String testMethodName, String runtimeTestName,
            long start, long end, double duration, RuntimeType testType,
            int slice, int testRunId, int exitStatus, ErrorInfo errorInfo) {
        this(recorder.getDbName(), recorder.getDbVersion(), recorder.hashCode(), coverage,
                testMethodName, runtimeTestName, start, end, duration, testType, slice, testRunId,
                exitStatus, errorInfo);
    }

    /**
     * See <code>PerTestRecordingTranscript#read(java.io.DataInputStream, org.openclover.core.CoverageDataSpec)</code>
     */
    @Override
    public String transcribe() throws IOException {
        File file = createCoverageFolderFor(fileOnDisk);
        try (DataOutputStream out = new DataOutputStream(IOStreamUtils.createDeflateOutputStream(file))) {
            header.write(out);
            out.writeUTF(testTypeName);
            out.writeUTF(testMethodName);
            out.writeUTF(runtimeTestName != null ? runtimeTestName : ""); // value might be null, protect against NPE
            out.writeInt(exitStatus);
            if (exitStatus == PerTestRecorder.ABNORMAL_EXIT) {
                IOStreamUtils.writeChars(exitMessage, out);
                IOStreamUtils.writeChars(stackTrace, out);
            }

            out.writeLong(start);
            out.writeDouble(duration);
            coverage.write(out);

            out.flush();
        } catch (IOException e) {
            Logger.getInstance().error("IO Exception flushing sliced coverage for recorder: " + fileOnDisk.getAbsolutePath(), e);
        }
        return file.getAbsolutePath();
    }

    public boolean get(int sliceIndex) {
        return coverage.member(sliceIndex);
    }

    @Override
    public String getTestTypeName() {
        return testTypeName;
    }

    @Override
    public String getTestMethodName() {
        return testMethodName;
    }

    @Override
    public String getRuntimeTestName() {
        return runtimeTestName;
    }

    @Override
    public String getExitMessage() {
        return exitMessage;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public int getExitStatus() {
        return exitStatus;
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return header.getWriteTimeStamp();
    }

    @Override
    public double getDuration() {
        return duration;
    }
    @Override
    public boolean hasResult() {
        return exitStatus != PerTestRecorder.NO_EXIT_RESULT;
    }

    @Override
    public boolean isResultPassed() {
        return exitStatus == PerTestRecorder.NORMAL_EXIT;
    }
}
