package org.openclover.core.recorder;

import clover.it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import clover.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import clover.it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import clover.it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import clover.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.api.registry.CoverageDataRange;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.util.SizedLRUCacheMap;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.CloverBitSet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;

/**
 * This model of per-test coverage does not retain all coverage data in memory but rather
 * keeps a reference to each per test coverage file on disk and loads on demand.
 * A capacity LRU cache is used to reduce disk IO.
 * <p/>
 * In order to maintain performance, it samples the first element (entry hit) of
 * each method in each FileInfo for the tests that that hit it then groups this
 * information at the file level. When coverage data for a range is requested, only the tests that hit
 * the relelvent FileInfo are loaded and queried thus, in most situations, avoiding a full scan
 * of all per-test coverage files for each query.
 *
 */
public class SamplingPerTestCoverage extends BasePerTestCoverage {
    private final FileInfoSample[] fileInfoSamples;
    private final Int2ObjectSortedMap fileIdxToSamplings;
    private final Int2ObjectMap tciIdsToRecordingFiles;
    private final Int2ObjectMap tciIdToTCIMap;
    private final CoverageDataSpec spec;
    private final SizedLRUCacheMap<String, CloverBitSet> coverageCache;
    private BitSet coverageMask;
    private BitSet passOnlyCoverageMask;

    /** A super-set of TCIs that covered a FileInfo at {@link #idx} through one or more of its methods */ 
    private static class FileInfoSample {
        private final int idx;
        private final int[] methodIdx;
        private final Set<TestCaseInfo> tcis;

        private FileInfoSample(int idx, int[] methodIdx) {
            this.idx = idx;
            this.methodIdx = methodIdx;
            this.tcis = newHashSet();
        }

        public void sample(TestCaseInfo tci, CloverBitSet coverage) {
            for (int idx : methodIdx) {
                if (coverage.member(idx)) {
                    tcis.add(tci);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public SamplingPerTestCoverage(Clover2Registry registry, CoverageDataSpec spec, int estPerTestRecordings) {
        super(registry.getDataLength());
        this.spec = spec;

        final List<FileInfoSample> fileSamples = newLinkedList();
        final IntArrayList methodIdx = new IntArrayList();
        registry.getProject().visitFiles(fileInfo -> {
            //Initialise a TCI sample for this FileInfo

            for(ClassInfo classInfo : fileInfo.getClasses()) {
                //Map method entry index back to the index in the TCI sample array
                for(MethodInfo methodInfo : classInfo.getMethods()) {
                    methodIdx.add(methodInfo.getDataIndex());
                }
            }

            fileSamples.add(
                    new FileInfoSample(
                            fileInfo.getDataIndex(),
                            methodIdx.toIntArray()));
            methodIdx.clear();
        });

        this.fileInfoSamples = fileSamples.toArray(new FileInfoSample[0]);
        Arrays.sort(this.fileInfoSamples, Comparator.comparingInt(fs -> fs.idx));

        this.fileIdxToSamplings = new Int2ObjectRBTreeMap();
        for (FileInfoSample fileSample : this.fileInfoSamples) {
            fileIdxToSamplings.put(fileSample.idx, fileSample);
        }

        this.tciIdsToRecordingFiles = new Int2ObjectOpenHashMap();
        this.tciIdToTCIMap = new Int2ObjectOpenHashMap();
        //We assume we know up-front the # of per-test recordings and only
        //resize if we exceed that (load = 1.1)
        this.coverageCache =
                new SizedLRUCacheMap<>(
                        spec.getPerTestStorageSize().getSizeInBytes(),
                        estPerTestRecordings,
                        1.1f);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addCoverage(TestCaseInfo tci, PerTestRecordingTranscript recording) {
        CloverBitSet coverage = recording.getCoverage();

        for (FileInfoSample fileInfoSample : fileInfoSamples) {
            fileInfoSample.sample(tci, coverage);
        }

        Set<String> pathsToCoverage = (Set<String>)tciIdsToRecordingFiles.get(tci.getId());
        if (pathsToCoverage == null) {
            pathsToCoverage = newHashSet();
            tciIdsToRecordingFiles.put(tci.getId(), pathsToCoverage);
        }
        pathsToCoverage.add(recording.getFile().getAbsolutePath());
        tciIdToTCIMap.put(tci.getId(), tci);
    }

    @Override
    public boolean hasPerTestData() {
        return !tciIdToTCIMap.isEmpty();
    }

    @Override
    public BitSet getAllHits() {
        if (coverageMask == null) {
            initMasks();
        }
        return (BitSet)coverageMask.clone();
    }

    private synchronized CloverBitSet getCoverageFor(String pathToCoverageFile) throws IOException {
        CloverBitSet coverage = coverageCache.get(pathToCoverageFile);
        if (coverage == null) {
            final File coverageFile = new File(pathToCoverageFile);
            coverage =
                RecordingTranscripts.readSliceFromDisk(
                        coverageFile.getParentFile(),
                        coverageFile.getName(),
                        spec).getCoverage();
            coverageCache.put(pathToCoverageFile, coverage);
        }
        return coverage;
    }

    @SuppressWarnings("unchecked")
    private BitSet getHitsFor(TestCaseInfo tci, BitSet result) {
        final Set<String> pathsToCoverage = (Set<String>)tciIdsToRecordingFiles.get(tci.getId());
        if (pathsToCoverage != null) {
            for (String pathToCoverage : pathsToCoverage) {
                try {
                    getCoverageFor(pathToCoverage).applyTo(result);
                } catch (IOException e) {
                    Logger.getInstance().error("Failed to load per-test coverage file \"" + pathToCoverage + "\"");
                }
            }
        }
        return result;
    }

    @Override
    public BitSet getHitsFor(TestCaseInfo tci) {
        return getHitsFor(tci, new BitSet(coverageSize));
    }

    @Override
    public BitSet getHitsFor(Set<TestCaseInfo> tcis) {
        BitSet result = new BitSet(coverageSize);
        for(TestCaseInfo tci : tcis) {
            getHitsFor(tci, result);
        }
        return result;
    }

    @Override
    public BitSet getHitsFor(Set<TestCaseInfo> tcis, CoverageDataRange range) {
        BitSet hits = getHitsFor(tcis);
        if (range != null) {
            BitSet mask = new BitSet();
            mask.set(range.getDataIndex(), range.getDataIndex() + range.getDataLength());
            hits.and(mask);
        }
        return hits;
    }

    @Override
    public BitSet getPassOnlyHits() {
        if (passOnlyCoverageMask == null) {
            initMasks();
        }
        return (BitSet)passOnlyCoverageMask.clone();
    }

    @Override
    public TestCaseInfo getTestById(int id) {
        return (TestCaseInfo)tciIdToTCIMap.get(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<TestCaseInfo> getTests() {
        return newHashSet(tciIdToTCIMap.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<TestCaseInfo> getTestsCovering(CoverageDataRange range) {
        final Set<TestCaseInfo> tcis = newHashSet();

        final int startIdx = range.getDataIndex();
        final int endIdx = range.getDataLength() + range.getDataIndex();
        //Get all sample indexes in the range
        Int2ObjectSortedMap subFileIdxToSamplings = fileIdxToSamplings.subMap(startIdx, endIdx + 1);
        //Get the previous one if the first index in the range isn't at the very start of the range
        if (subFileIdxToSamplings.isEmpty() || subFileIdxToSamplings.firstIntKey() != startIdx) {
            final Int2ObjectSortedMap head = fileIdxToSamplings.headMap(startIdx);
            if (!head.isEmpty()) {
                subFileIdxToSamplings = new Int2ObjectRBTreeMap(subFileIdxToSamplings);
                subFileIdxToSamplings.put(head.lastIntKey(), head.get(head.lastIntKey()));
            }
        }

        //Add all TCIs sampled in range
        for (FileInfoSample fileInfoSample : (Iterable<FileInfoSample>) subFileIdxToSamplings.values()) {
            tcis.addAll(fileInfoSample.tcis);
        }

        //Clip any TCI which does not have any coverage in the precise range as specified
        for (Iterator<TestCaseInfo> tciIter = tcis.iterator(); tciIter.hasNext();) {
            Set<String> pathToCoverageFiles = (Set<String>)tciIdsToRecordingFiles.get(tciIter.next().getId());

            nextTci: {
                if (pathToCoverageFiles != null) {
                    for (String pathToCoverageFile : pathToCoverageFiles) {
                        try {
                            CloverBitSet bitSet = getCoverageFor(pathToCoverageFile);
                            int firstBitSetAfterRange = bitSet.nextSetBit(startIdx);
                            if (firstBitSetAfterRange >= startIdx && firstBitSetAfterRange < endIdx) {
                                break nextTci;
                            }
                        } catch (IOException e) {
                            Logger.getInstance().warn(
                                String.format(
                                    "Failed to load coverage file: \"%s\" while calculating hits in range [%d,%d]",
                                    pathToCoverageFile, range.getDataIndex(), range.getDataIndex() + range.getDataLength()),
                                e);
                        }
                    }
                    tciIter.remove();
                }
            }
        }
        return tcis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<TestCaseInfo, BitSet> mapTestsAndCoverageForFile(FullFileInfo fileInfo) {
        final Map<TestCaseInfo, BitSet> tcisAndCoverage = newHashMap();

        FileInfoSample fileSample = (FileInfoSample)fileIdxToSamplings.get(fileInfo.getDataIndex());

        if (fileSample != null) {
            final int minBitSize = fileInfo.getDataIndex() + fileInfo.getDataLength();

            for (final TestCaseInfo tci : fileSample.tcis) {
                final BitSet totalTciCoverage = new BitSet(minBitSize);
                final Collection<String> pathToCoverageFiles = (Set<String>) tciIdsToRecordingFiles.get(tci.getId());
                if (pathToCoverageFiles != null) {
                    for (String pathToCoverageFile : pathToCoverageFiles) {
                        try {
                            final CloverBitSet coverageForFile = getCoverageFor(pathToCoverageFile);
                            tcisAndCoverage.put(tci, coverageForFile.applyTo(totalTciCoverage));
                        } catch (IOException e) {
                            Logger.getInstance().warn(
                                    String.format(
                                            "Failed to load coverage file: \"%s\" while calculating hits in range [%d,%d]",
                                            pathToCoverageFile, fileInfo.getDataIndex(), fileInfo.getDataIndex() + fileInfo.getDataLength()),
                                    e);
                        }
                    }
                }
            }
        }
        return tcisAndCoverage;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initMasks() {
        final BitSet coverageMask = new BitSet(coverageSize);
        final BitSet passOnlyCoverageMask = new BitSet(coverageSize);
        //All coverage is unique to start with until later proven otherwise
        final BitSet coverageNotUniqueMask = new BitSet(coverageSize);

        for (final Map.Entry<Integer, Set<String>> entry : (Iterable<Map.Entry<Integer, Set<String>>>) tciIdsToRecordingFiles.entrySet()) {
            final boolean success = ((TestCaseInfo) tciIdToTCIMap.get(entry.getKey().intValue())).isSuccess();
            final Set<String> pathsToCoverageFiles = entry.getValue();
            if (pathsToCoverageFiles.size() > 0) {
                for (String pathToCoverageFile : pathsToCoverageFiles) {
                    try {
                        final CloverBitSet coverage = getCoverageFor(pathToCoverageFile);
                        for (int i = coverage.nextSetBit(0); i >= 0; i = coverage.nextSetBit(i + 1)) {
                            if (success) {
                                passOnlyCoverageMask.set(i);
                            }
                            if (coverageMask.get(i)) {
                                coverageNotUniqueMask.set(i);
                            } else {
                                coverageMask.set(i);
                            }
                        }
                    } catch (IOException e) {
                        Logger.getInstance().warn(
                                String.format("Failed to load coverage file while calculating unique coverage mask: \"%s\"", pathToCoverageFile), e);
                    }
                }
            }
        }

        coverageNotUniqueMask.flip(0, coverageNotUniqueMask.size());
        this.coverageMask = coverageMask;
        this.uniqueCoverageMask = coverageNotUniqueMask;
        this.passOnlyCoverageMask = passOnlyCoverageMask;
    }
}
