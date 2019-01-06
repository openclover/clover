package com.atlassian.clover;

import clover.org.apache.commons.lang3.mutable.MutableLong;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.recorder.GlobalCoverageRecordingTranscript;
import com.atlassian.clover.recorder.PerTestRecordingTranscript;
import com.atlassian.clover.recorder.RecordingTranscripts;
import com.atlassian.clover.registry.entities.BaseFileInfo;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.FileInfoVisitor;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.collections.Pair;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.Collection;


public class CoverageDataCollator {
    private Clover2Registry registry;
    private RecordingTranscripts.Filter filter;

    /**
     * load coverage data recorded between the timestamps provided
     *
     * @param registry the registry to load with data
     */
    public CoverageDataCollator(Clover2Registry registry) {
        this.registry = registry;
    }

    /** Collator with a pre-created filter */
    public CoverageDataCollator(Clover2Registry registry, CoverageDataSpec spec) {
        this.registry = registry;
        this.filter =
            new RecordingTranscripts.Filter(
                FileUtils.getCurrentDirIfNull(registry.getRegistryFile().getParentFile()),
                registry.getRegistryFile().getName(), registry.getVersion() - spec.getSpan(), Long.MAX_VALUE, spec.isDeleteUnusedCoverage(), spec.isLoadPerTestData());
    }

    public CoverageDataCollator copyWithNewRegistry(Clover2Registry registry) {
        final CoverageDataCollator collator = new CoverageDataCollator(registry);
        collator.filter = filter;
        return collator;
    }

    public CoverageData loadCoverageData(CoverageDataSpec spec, ProgressListener progressListener) throws CloverException {
        return loadCoverageData(null, spec, progressListener);
    }

    public CoverageData loadCoverageData(CoverageData coverageData, CoverageDataSpec spec, ProgressListener progressListener) throws CloverException {
        final RecordingTranscripts.Filter prevFilter = filter;
        final RecordingTranscripts.Filter newFilter = updateFilter(prevFilter, spec);
        //Loaded previous, if so, just update
        if (newFilter == prevFilter && coverageData != null) {
            coverageData = new CoverageData(registry, coverageData, spec);
            final Pair<Set<RecordingTranscripts.FileRef>, Set<RecordingTranscripts.FileRef>> newRecordings =
                newFilter.collectUnseenFilesAnd(prevFilter);
            collateRecordingFiles(newRecordings.first, coverageData, spec);
            if (spec.isLoadPerTestData()) {
                collatePerTestRecordings(
                    newRecordings.second, coverageData, spec, progressListener != null ? progressListener : ProgressListener.NOOP_LISTENER);
            }
        } else {
            newFilter.collectAllFiles();
            coverageData = new CoverageData(registry, spec);
            collateRecordingFiles(newFilter.getCoverageRecordingFiles(), coverageData, spec);
            if (spec.isLoadPerTestData()) {
                collatePerTestRecordings(
                    newFilter.getPerTestRecordingFiles(), coverageData, spec, progressListener != null ? progressListener : ProgressListener.NOOP_LISTENER);
            }
        }

        this.filter = newFilter;
        return coverageData;
    }

    public boolean isOutOfDate() {
        return filter == null || filter.isOutOfDate();
    }

    private RecordingTranscripts.Filter updateFilter(RecordingTranscripts.Filter prevFilter, CoverageDataSpec spec) {
        final long from = registry.getVersion() - spec.getSpan();
        final long to = Long.MAX_VALUE;
        final boolean loadPerTestData = spec.isLoadPerTestData();
        return (prevFilter == null || prevFilter.getFrom() != from || prevFilter.getTo() != to || prevFilter.isLoadPerTestData() != loadPerTestData) ?
                new RecordingTranscripts.Filter(
                    FileUtils.getCurrentDirIfNull(registry.getRegistryFile().getParentFile()),
                    registry.getRegistryFile().getName(), from, to, spec.isDeleteUnusedCoverage(), loadPerTestData)
                : prevFilter;
    }

    private void collateRecordingFiles(Collection<RecordingTranscripts.FileRef> files, final CoverageData coverageData, final CoverageDataSpec spec) {
        final long start = System.currentTimeMillis();

        int numRecordings = 0;
        long tsNewestRecordingUsed = 0;
        long tsOldestRecordingUsed = Long.MAX_VALUE;

        //An optimisation - ignore coverage which was generated for versions of files
        //that no longer exist.
        final MutableLong maxVersion = new MutableLong(0L);
        final MutableLong minVersion = new MutableLong(Long.MAX_VALUE);
        registry.getProject().visitFiles(new FileInfoVisitor() {
            @Override
            public void visitFileInfo(BaseFileInfo f) {
                FullFileInfo file = (FullFileInfo)f;
                maxVersion.setValue(Math.max(maxVersion.longValue(), file.getMaxVersion()));
                minVersion.setValue(Math.min(minVersion.longValue(), file.getMinVersion()));
            }
        });

        logSourceFileTimeStamps(minVersion.longValue(), maxVersion.longValue());
        logInstrumentationSessionVersions();

        for (final RecordingTranscripts.FileRef recordingFile : files) {
            //Ensure we complain only once per recording file
            final boolean[] alreadyTruncated = new boolean[]{false};
            try {
                final GlobalCoverageRecordingTranscript rec = (GlobalCoverageRecordingTranscript) recordingFile.read(spec);
                long version = rec.getDbVersion();

                logGlobalRecordingFileVersion(recordingFile, rec);

                if (version <= maxVersion.longValue() && version >= minVersion.longValue()) {
                    coverageData.addCoverage(rec);
                    numRecordings++;
                    // oldest is based on the init time of the recorder
                    if (recordingFile.getTimestamp() < tsOldestRecordingUsed) {
                        tsOldestRecordingUsed = recordingFile.getTimestamp();
                    }

                    // newest is based on the last write timestamp
                    if (rec.getWriteTimeStamp() > tsNewestRecordingUsed) {
                        tsNewestRecordingUsed = rec.getWriteTimeStamp();
                    }
                } else {
                    Logger.getInstance().verbose("Ignoring coverage recording " + rec + " because no FileInfo supports its coverage range");
                }
            } catch (IOException e) {
                Logger.getInstance().warn("Failed to load coverage recording " + recordingFile, e);
            }
        }
        coverageData.avoidObviousOverflow();
        coverageData.setTimestamp(tsNewestRecordingUsed);

        final long end = System.currentTimeMillis();
        Logger.getInstance().debug("Processed " + numRecordings + " recording files in " + (end - start) + "ms (" + (numRecordings != 0 ? "" + (end - start) / numRecordings + "ms" : "-") + " per recording)");
    }

    private void collatePerTestRecordings(Collection<RecordingTranscripts.FileRef> perTestRecordings, final CoverageData coverageData, final CoverageDataSpec spec, ProgressListener progressListener) throws CloverException {
        final long start = System.currentTimeMillis();

        int numPerTestRecordings = 0;
        float progress = 0;
        final float progressIncrement = perTestRecordings.size() > 0 ? 1f / perTestRecordings.size() : 1f;

        for (final RecordingTranscripts.FileRef recordingFile : perTestRecordings) {
            numPerTestRecordings++;

            try {
                final PerTestRecordingTranscript recording = (PerTestRecordingTranscript) recordingFile.read(spec);
                final TestCaseInfo tci = TestCaseInfo.Factory.getInstanceForSlice(recording);
                coverageData.addCoverage(tci, recording);
            } catch (Exception e) {
                Logger.getInstance().verbose("Failed to load per-test coverage recording " + recordingFile, e);
            }
            progress += progressIncrement;
            progressListener.handleProgress("Reading per-test data", progress);
        }

        final long end = System.currentTimeMillis();
        Logger.getInstance().debug("Processed " + numPerTestRecordings + " per-test recording files in " + (end - start) + "ms (" + (numPerTestRecordings != 0 ? "" + (end - start) / numPerTestRecordings + "ms" : "-") + " per recording)");
    }

    private void logSourceFileTimeStamps(long minVersion, long maxVersion) {
        Logger.getInstance().verbose("Source files timestamps:");
        Logger.getInstance().verbose("  minVersion=" + minVersion + " (" + new Date(minVersion) + ")\n"
                + "  maxVersion=" + maxVersion + " (" + new Date(maxVersion) + ")");
    }

    private void logInstrumentationSessionVersions() {
        if (!Logger.canIgnore(Logger.LOG_VERBOSE)) {
            String sessionTimestamps = "";
            int i = 0;
            Logger.getInstance().verbose("Instrumentation sessions:");
            for (Object obj : registry.getInstrHistory()) {
                Clover2Registry.InstrumentationInfo session = (Clover2Registry.InstrumentationInfo)obj;
                sessionTimestamps += "  " + i + ": version " + session.getVersion() + " (" + new Date(session.getVersion()) + ")\n";
                i++;
            }
            Logger.getInstance().verbose(sessionTimestamps);
        }
    }

    private void logGlobalRecordingFileVersion(final RecordingTranscripts.FileRef recordingFile,
                                               final GlobalCoverageRecordingTranscript rec) {
        Logger.getInstance().verbose("Global recording file:");
        Logger.getInstance().verbose("  file=" + recordingFile.toString()
                + "\n  dbVersion=" + rec.getDbVersion() + " (" + new Date(rec.getDbVersion()) + ")"
                + "\n  writeTimeStamp=" + rec.getWriteTimeStamp() + " (" + new Date(rec.getWriteTimeStamp()) + ")");
    }

}