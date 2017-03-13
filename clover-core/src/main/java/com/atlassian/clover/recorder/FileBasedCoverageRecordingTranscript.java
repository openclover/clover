package com.atlassian.clover.recorder;

import clover.org.apache.commons.lang3.mutable.MutableLong;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.util.CoverageUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

public class FileBasedCoverageRecordingTranscript extends BaseCoverageRecording implements GlobalCoverageRecordingTranscript {
    protected int[] hitCounts;
    protected long coverageSum;

    public FileBasedCoverageRecordingTranscript(Header header, File fileOnDisk) {
        super(header, fileOnDisk);
    }

    @Override
    public long getCoverageSum() {
        return coverageSum;
    }

    @Override
    public void read(DataInputStream in, CoverageDataSpec spec) throws IOException {
        MutableLong sum = new MutableLong(0);
        hitCounts = CoverageUtils.readCoverageAndSumCoverage(in, sum);
        coverageSum = sum.longValue();
    }

    @Override
    public int get(int slotIndex) {
        return hitCounts[slotIndex];
    }

    @Override
    public int getCount() {
        return hitCounts.length;
    }

    @Override
    public int addTo(int[] coverage) {
        int max = Math.min(hitCounts.length, coverage.length);

        for (int i = 0; i < max; i++) {
            coverage[i] += hitCounts[i];
        }
        return max;
    }

    @Override
    public String toString() {
        return "FileBasedCoverageRecordingTranscript[" +
            "header=" + header +
            ", coverageSum=" + coverageSum +
            ", hitCounts.length=" + (hitCounts == null ? null : hitCounts.length) +
            ']';
    }
}
