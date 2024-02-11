package com.atlassian.clover.recorder;

import com.atlassian.clover.CoverageDataSpec;
import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.RuntimeType;
import org.openclover.runtime.RuntimeType;
import org.openclover.runtime.recorder.BaseCoverageRecording;
import org.openclover.runtime.recorder.PerTestRecorder;
import org.openclover.runtime.recorder.PerTestRecording;
import org.openclover.runtime.util.CloverBitSet;
import com.atlassian.clover.util.CloverUtils;
import com.atlassian.clover.util.FileUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;

public class PerTestRecordingTranscript extends BaseCoverageRecording implements PerTestRecording, RecordingTranscript {
    private CloverBitSet coverage;
    private String testTypeName;
    private String testMethodName;
    private String runtimeTestName;
    private String exitMessage;
    private String stackTrace;
    private int exitStatus = -1;
    private long start;
    private double duration;

    public PerTestRecordingTranscript(Header header, File fileOnDisk) {
        super(header, fileOnDisk);
    }

    public PerTestRecordingTranscript(CloverBitSet coverage, File fileOnDisk, long dbVersion,
                                      String testMethodName, String runtimeTestName,
                                      long start, long end, double duration, RuntimeType testType,
                                      int slice, int testRunId, int exitStatus, ErrorInfo errorInfo) {
        super(new BaseCoverageRecording.Header(dbVersion, end, FORMAT), fileOnDisk);
        this.coverage = coverage;
        this.testMethodName = testMethodName;
        this.runtimeTestName = runtimeTestName;
        this.testTypeName = testType.name;
        this.exitStatus = exitStatus;
        this.exitMessage = errorInfo == null ? null : errorInfo.getMessage();
        this.stackTrace = errorInfo == null ? null : errorInfo.getStackTrace();
        this.start = start;
        this.duration = duration;
    }

    /**
     * @see com.atlassian.clover.recorder.FileBasedPerTestRecording#transcribe()
     */
    @Override
    public void read(DataInputStream in, CoverageDataSpec spec) throws IOException {
        testTypeName = in.readUTF().replace('$', '.'); // hack - see CCD-294
        testMethodName = in.readUTF();
        runtimeTestName = in.readUTF();
        if (runtimeTestName.length() == 0) { // see transcribe()
            runtimeTestName = null;
        }
        exitStatus = in.readInt();
        if (exitStatus == PerTestRecorder.ABNORMAL_EXIT) {
            exitMessage = FileUtils.readChars(in);
            stackTrace = CloverUtils.transformStackTrace(FileUtils.readChars(in), spec.isFilterTraces());
        }
        start = in.readLong();
        duration = in.readDouble();
        coverage = CloverBitSet.read(in);
    }

    CloverBitSet getCoverage() {
        return coverage;
    }
    
    public boolean get(int slotIndex) {
        return coverage.member(slotIndex);
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

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public String getExitMessage() {
        return exitMessage;
    }

    public String toString() {
        return "PerTestRecordingTranscript[" +
            "coverage.size=" + coverage.size() +
            ", testTypeName='" + testTypeName + '\'' +
            ", testMethodName='" + testMethodName + '\'' +
            ", exitMessage='" + exitMessage + '\'' +
            ", stackTrace='" + stackTrace + '\'' +
            ", exitStatus=" + ((exitStatus == PerTestRecorder.ABNORMAL_EXIT) ? "'Abnormal'" : (exitStatus == PerTestRecorder.NO_EXIT_RESULT) ? "'None'" : "'Normal'") +
            ", start=" + start +
            ']';
    }

    public void applyTo(BitSet hits) {
        coverage.applyTo(hits);
}
}
