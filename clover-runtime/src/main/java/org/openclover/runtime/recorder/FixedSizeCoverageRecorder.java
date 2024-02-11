package org.openclover.runtime.recorder;

import org.openclover.runtime.registry.RegistryFormatException;
import org.openclover.runtime.registry.format.RegAccessMode;
import org.openclover.runtime.registry.format.RegHeader;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.CloverBitSet;
import com_atlassian_clover.CoverageRecorder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public final class FixedSizeCoverageRecorder extends BaseCoverageRecorder {
    private static final Set<String> TRUNC_WARNING_DBS = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> MERGE_WARNING_DBS = Collections.synchronizedSet(new HashSet<>());

    private final int[] elements;

    /**
     * Factory method. Use this to get an instance of the recorder. Do not call constructors directly
     * (they're not private only for the sake of unit tests).
     */
    public static CoverageRecorder createFor(final File dbFile, final long dbVersion, final int maxNumElements, final long cfgbits) throws IOException, RegistryFormatException {
        final RegHeader header = RegHeader.readFrom(dbFile);
        if (header.getAccessMode() == RegAccessMode.READWRITE) {
            final int numElementsInDb = header.getSlotCount();
            return recorderBigEnoughFor(
                    dbFile.getAbsolutePath(),
                    maxNumElements,
                    numElementsInDb,
                    new NewRecorderBlock() {
                        @Override
                        public CoverageRecorder call() {
                            return new FixedSizeCoverageRecorder(dbFile.getAbsolutePath(), dbVersion, numElementsInDb, cfgbits);
                        }
                    });
        } else {
            if (!MERGE_WARNING_DBS.contains(dbFile.getAbsolutePath())) {
                MERGE_WARNING_DBS.add(dbFile.getAbsolutePath());
                Logger.getInstance().warn(
                        "CLOVER: Clover database: '" + dbFile.getAbsolutePath() + "' can only be used for reporting because it is the result of a merge.");
                Logger.getInstance().warn("CLOVER: Coverage data for some classes will not be gathered.");
            }
            return NullRecorder.INSTANCE;
        }
    }

    /*private*/ FixedSizeCoverageRecorder(String dbName, long dbVersion, int numElements, long cfgbits) {
        this(dbName, dbVersion, numElements, cfgbits, GlobalRecordingWriteStrategy.WRITE_TO_FILE);
    }

    /*private*/ FixedSizeCoverageRecorder(String dbName, long dbVersion, int numElements, long cfgbits, GlobalRecordingWriteStrategy writeStrategy) {
        super(dbName, dbVersion, cfgbits, writeStrategy);
        this.elements = new int[numElements];
    }

    @Override
    public CloverBitSet compareCoverageWith(CoverageSnapshot before) {
        final int[] beforeElements = before.getCoverage()[0];
        for(int i = 0; i < beforeElements.length; i++) {
            beforeElements[i] = beforeElements[i] - elements[i];
        }
        return CloverBitSet.forHits(beforeElements);
    }

    @Override
    public CloverBitSet createEmptyHitsMask() {
        return new CloverBitSet(elements.length);
    }

    /**
     * Increment slot at index
     */
    @Override
    public void inc(int index) {
        testCoverage.set(index);
        elements[index]++;
    }

    /**
     * @return coverage for slot at index but increment by one before evaluation
     */
    @Override
    public int iget(int index) {
        testCoverage.set(index);
        return ++elements[index];
    }

    @Override
    protected String write() throws IOException {
        return write(new int[][] {this.elements}, this.elements.length);
    }

    @Override
    public CoverageRecorder withCapacityFor(int maxNumElements) {
        return recorderBigEnoughFor(dbName, maxNumElements, elements.length, new NewRecorderBlock() {
            @Override
            public CoverageRecorder call() {
                return FixedSizeCoverageRecorder.this;
            }
        });
    }

    @Override
    public CoverageSnapshot getCoverageSnapshot() {
        return new CoverageSnapshot(new int[][] {elements.clone()});
    }

    private static CoverageRecorder recorderBigEnoughFor(String dbName, int numRequiredElements, int numAvailableElements, NewRecorderBlock recorderIfSufficient) {
        if (numRequiredElements > numAvailableElements) {
            logInsufficientCapacity(dbName, numRequiredElements, numAvailableElements);
            return NullRecorder.INSTANCE;
        } else {
            return recorderIfSufficient.call();
        }
    }

    private static void logInsufficientCapacity(String dbName, int numRequiredElements, int numAvailableElements) {
        if (!TRUNC_WARNING_DBS.contains(dbName)) {
            TRUNC_WARNING_DBS.add(dbName);
            Logger.getInstance().warn(
                "CLOVER: Clover database: '" + dbName + "' is no longer valid. Min required size for currently loading class: " +
                numRequiredElements + ", actual size: " + numAvailableElements);
            Logger.getInstance().warn("CLOVER: Coverage data for some classes will not be gathered.");
        }
    }

    private interface NewRecorderBlock extends Callable<CoverageRecorder> {
        @Override
        CoverageRecorder call();
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "FixedSizeCoverageRecorder[elements.length=" + elements.length + "]";
    }
    ///CLOVER:ON
}
