package com.atlassian.clover.recorder;

import com.atlassian.clover.CoverageDataSpec;
import org.openclover.runtime.Logger;
import org.openclover.runtime.recorder.BaseCoverageRecording;
import org.openclover.runtime.recorder.GlobalCoverageRecording;
import org.openclover.runtime.util.IOStreamUtils;
import com.atlassian.clover.util.collections.Pair;
import org.openclover.runtime.recorder.CoverageRecording;

import java.io.DataInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.util.Maps.newHashMap;
import static org.openclover.util.Sets.newHashSet;

public class RecordingTranscripts {
    public static final String NUM_R36 = "([0-9a-z]+)";
    public static final String STD_REC_SUFFIX = NUM_R36 + "_" + NUM_R36;
    public static final String SLICE_SUFFIX = NUM_R36 + "_" + NUM_R36 + "_" + STD_REC_SUFFIX + ".s";
    public static Pattern stdRecordingSuffix = Pattern.compile(STD_REC_SUFFIX);
    public static Pattern sliceRecordingSuffix = Pattern.compile(SLICE_SUFFIX);

    public static GlobalCoverageRecordingTranscript readCoverageFromDisk(File file, CoverageDataSpec spec) throws IOException {
        return readCoverageFromDisk(file.getParentFile(), file.getName(), spec);
    }

    public static GlobalCoverageRecordingTranscript readCoverageFromDisk(File dir, String file, CoverageDataSpec spec) throws IOException {
        File inf = new File(dir, file);
        File alt = new File(dir, file + GlobalCoverageRecording.ALT_SUFFIX);

        DataInputStream in = null;
        DataInputStream altIn = null;
        GlobalCoverageRecordingTranscript rec = null;
        GlobalCoverageRecordingTranscript altRec = null;
        IOException lastError = null;
        try {
            // read the rec header
            try {
                in = new DataInputStream(IOStreamUtils.createInflaterInputStream(inf));
                BaseCoverageRecording.Header header = new BaseCoverageRecording.Header(in);
                Logger.getInstance().debug("Read header for \"" + inf + "\": " + header);
                rec = new FileBasedCoverageRecordingTranscript(header, inf);
            }
            catch (IOException e) {
                rec = null;
                lastError = e;
                Logger.getInstance().verbose("Error reading \"" + inf + "\": " + e + ", skipped.");
                if (!alt.exists()) {
                    // no alternate exists
                    throw e;
                }
            }

            // if the alt exists, read its header
            if (alt.exists()) {
                try {
                    altIn = new DataInputStream(IOStreamUtils.createInflaterInputStream(alt));
                    BaseCoverageRecording.Header header = new BaseCoverageRecording.Header(altIn);
                    Logger.getInstance().debug("Read header for \"" + alt + "\": " + header);
                    altRec = new FileBasedCoverageRecordingTranscript(header, alt);
                }
                catch (IOException e) {
                    altRec = null;
                    lastError = e;
                    Logger.getInstance().verbose("Error reading \"" + alt + "\": " + e + ", skipped.");
                    if (rec == null) {
                        // neither record header could be read. bail
                        throw e;
                    }
                }
            }

            if (altRec != null && (rec == null || altRec.getWriteTimeStamp() > rec.getWriteTimeStamp())) {
                try {
                    altRec.read(altIn, spec);
                    Logger.getInstance().debug("Read data for file \"" + alt + "\": " + altRec);
                    return altRec;
                }
                catch (IOException e) {
                    lastError = e;
                    Logger.getInstance().verbose("Error reading data of \"" + alt + "\": " + e + ", skipped.");
                    // fall thru to try reading rec
                }
            }
            if (rec != null) {
                try {
                    rec.read(in, spec);
                    Logger.getInstance().debug("Read data for file \"" + inf + "\": " + rec);
                    return rec;
                }
                catch (IOException e) {
                    lastError = e;
                    Logger.getInstance().verbose("Error reading data of \"" + inf + "\": " + e + ", skipped.");
                    if (altRec != null) {
                        // try reading alt as a backup
                        // exception allowed to escape here; nothing else to do
                        altRec.read(altIn, spec);
                        Logger.getInstance().debug("Read data for \"" + alt + "\"");
                        return altRec;
                    }
                }
            }

            // failed to read either.
            throw new IOException(lastError.getClass().getName());

        } finally {
            IOStreamUtils.close(in);
            IOStreamUtils.close(altIn);
        }
    }

    public static PerTestRecordingTranscript readSliceFromDisk(File dir, String file, CoverageDataSpec spec) throws IOException {
        File inf = new File(dir, file);
        DataInputStream in = null;

        try {
            in = new DataInputStream(IOStreamUtils.createInflaterInputStream(inf));
            BaseCoverageRecording.Header header = new BaseCoverageRecording.Header(in);
            Logger.getInstance().debug("Read header for \"" + inf + "\": " + header);
            PerTestRecordingTranscript rec = new PerTestRecordingTranscript(header, inf);
            rec.read(in, spec);
            Logger.getInstance().debug("Recording data for file \"" + inf + "\": " + rec);
            return rec;
        }
        catch (IOException e) {
            Logger.getInstance().verbose("Error reading \"" + inf + "\": " + e + ", skipped.");
            throw e;
        } finally {
            IOStreamUtils.close(in);
        }
    }

    public static FileRef fromFile(File dir, String filename, String dbname) {
        int baselength = dbname.length();
        try {
            if (filename.startsWith(dbname) && (filename.length() > baselength)) {

                int tsMarker = filename.lastIndexOf("_");
                if (tsMarker < baselength || (tsMarker == filename.length() - 1) ||
                    filename.endsWith(GlobalCoverageRecording.ALT_SUFFIX)) {
                    // context definition files and
                    // alt rec files are not visible here
                    return null;
                }

                FileRef recfile = new FileRef();
                String suffix = filename.substring(baselength);
                Matcher m = sliceRecordingSuffix.matcher(suffix);

                recfile.datafile = new File(dir, filename);

                if (m.matches()) {
                    recfile.testRecording = true;
                    recfile.typedTestId = Long.parseLong(m.group(1), 36);
                    recfile.runId = Long.parseLong(m.group(2), 36);
                    recfile.hash = Long.parseLong(m.group(3), 36);
                    recfile.timestamp = Long.parseLong(m.group(4), 36);
                    return recfile;
                }

                m = stdRecordingSuffix.matcher(suffix);

                if (m.matches()) {
                    recfile.hash = Long.parseLong(m.group(1), 36);
                    recfile.timestamp = Long.parseLong(m.group(2), 36);
                    return recfile;
                }
            }
        }
        catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            // ignore
        }

        return null;

    }

    public static class Filter {
        private final File dir;
        private final String basename;
        private final long from;
        private final long to;
        private final boolean deleteExcluded;
        private final boolean loadPerTestData;
        private final Map<String, FileRef> perTestFiles = newHashMap();
        private final Map<String, FileRef> recordingFiles = newHashMap();

        public Filter(File dir, String basename, long from, long to, boolean deleteExcluded, boolean loadPerTestData) {
            this.dir = dir;
            this.basename = basename;
            this.from = from;
            this.to = to;
            this.deleteExcluded = deleteExcluded;
            this.loadPerTestData = loadPerTestData;
        }

        public void collectAllFiles() {
            collectUnseenFilesAnd(null);
        }

        /**
         * Collates all recording files but tracks those that weren't previously
         * seen via the orig parameter.
         * @param orig the filter that was last used
         * @return a pair of sets of FileRefs reflecting the recording and per-test recording files
         * that were found since the original scan
         */
        public Pair<Set<FileRef>, Set<FileRef>> collectUnseenFilesAnd(Filter orig) {
            final Map<String, FileRef> origRecordingFiles = orig == null ? Collections.<String, FileRef>emptyMap() : orig.recordingFiles;
            final Map<String, FileRef> origPerTestFiles = orig == null ? Collections.<String, FileRef>emptyMap() : orig.perTestFiles;

            final Map<String, FileRef> newRecordingFiles = newHashMap();
            final Map<String, FileRef> newPerTestFiles = newHashMap();

            dir.list((dir, name) -> {
                FileRef recfile = fromFile(dir, name, basename);
                if (recfile != null) {
                    final String path = recfile.getDatafile().getAbsolutePath();
                    if (recfile.getTimestamp() >= from && recfile.getTimestamp() <= to) {
                        if (recfile.isTestRecording()) {
                            if (loadPerTestData && !origPerTestFiles.containsKey(path)) {
                                perTestFiles.put(path, recfile);
                                newPerTestFiles.put(path, recfile);
                            }
                        } else {
                            // a normal recording file
                            if (!origRecordingFiles.containsKey(path)) {
                                recordingFiles.put(path, recfile);
                                newRecordingFiles.put(path, recfile);
                            }
                        }
                        return true;
                    } else {
                        Logger.getInstance().debug(
                            (deleteExcluded ? "deleting" : "ignoring") + " out of date coverage recording file: " + name + ", timestamp " +
                            (to < Long.MAX_VALUE ? "not in range " + from + "-" + to : "< " + from));
                        if (deleteExcluded) {
                            recfile.getDatafile().delete();
                        }
                    }
                }
                return false;
            });
            recordingFiles.putAll(origRecordingFiles);
            perTestFiles.putAll(origPerTestFiles);

            return Pair.<Set<FileRef>, Set<FileRef>>of(
                    newHashSet(newRecordingFiles.values()),
                    newHashSet(newPerTestFiles.values()));
        }
        
        public Set<FileRef> getPerTestRecordingFiles() {
            return newHashSet(perTestFiles.values());
        }

        public Set<FileRef> getCoverageRecordingFiles() {
            return newHashSet(recordingFiles.values());
        }


        public long getFrom() {
            return from;
        }

        public long getTo() {
            return to;
        }

        public String getBasename() {
            return basename;
        }

        public File getDir() {
            return dir;
        }

        public boolean isLoadPerTestData() {
            return loadPerTestData;
        }

        public boolean isOutOfDate() {
            RecordingTranscripts.Filter latestFilter =
                new RecordingTranscripts.Filter(dir, basename, from, to, false, false); // we don't care about per-test files

            latestFilter.collectAllFiles();

            return
                !getCoverageRecordingFiles().equals(
                    latestFilter.getCoverageRecordingFiles());
        }
    }

    public final static class FileRef implements Comparable {
        private boolean testRecording;
        private long typedTestId = -1;
        private long runId;
        private long hash;
        private long timestamp;
        private File datafile;

        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            FileRef other = (FileRef)object;

            if (hash != other.hash) return false;
            if (testRecording != other.testRecording) return false;
            if (timestamp != other.timestamp) return false;
            if (typedTestId != other.typedTestId) return false;
            if (runId != other.runId) return false;
            return datafile.equals(other.datafile);
        }

        public int hashCode() {
            int result;
            result = (testRecording ? 1 : 0);
            result = 31 * result + (int)(typedTestId ^ (typedTestId >>> 32));
            result = 31 * result + (int)(runId ^ (runId >>> 32));
            result = 31 * result + (int)(hash ^ (hash >>> 32));
            result = 31 * result + (int)(timestamp ^ (timestamp >>> 32));
            result = 31 * result + datafile.hashCode();
            return result;
        }


        @Override
        public int compareTo(Object object) {
            if (this == object) return 0;
            if (object == null || getClass() != object.getClass()) return 1;

            FileRef other = (FileRef)object;

            if (datafile.compareTo(other.datafile) != 0) return datafile.compareTo(other.datafile);
            if (timestamp > other.timestamp) return 1;
            if (timestamp < other.timestamp) return -1;
            if (hash > other.hash) return 1;
            if (hash < other.hash) return -1;
            if (testRecording != other.testRecording) return testRecording ? 1 : -1;
            return Long.compare(typedTestId, other.typedTestId);
        }

        public CoverageRecording read(CoverageDataSpec spec) throws IOException {
            if (testRecording) {
                return readSliceFromDisk(getDatafile().getParentFile(), getDatafile().getName(), spec);
            } else {
                return readCoverageFromDisk(getDatafile().getParentFile(), getDatafile().getName(), spec);
            }
        }

        public boolean isTestRecording() {
            return testRecording;
        }

        public long getTypedTestId() {
            return typedTestId;
        }

        public int getTestId() {
            return (int)typedTestId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public File getDatafile() {
            return datafile;
        }

        public long getRunId() {
            return runId;
        }

        public long getHash() {
            return hash;
        }

        @Override
        public String toString() {
            return "RecordingTranscripts.FileRef[" +
                "datafile=" + datafile +
                ", testRecording=" + testRecording +
                ", typedTestId=" + typedTestId +
                ", runId=" + runId +
                ", hash=" + hash +
                ", timestamp=" + timestamp +
                ']';
        }
    }
}
