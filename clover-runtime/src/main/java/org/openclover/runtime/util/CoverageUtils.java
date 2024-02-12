package org.openclover.runtime.util;

import org.openclover.runtime.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;


public class CoverageUtils {
    public static final int RLE_RUN_MARKER = -1;
    public static final int RLE_RUN_THRESHOLD = 3;

    public static void rleCompressAndWriteCoverage(DataOutputStream out, int[][] coverage, int numElements) throws IOException {
        int elementsWritten = 0;
        int intsWritten = 0;
        boolean inRun = false;

        int runCount = 0;
        int runValue = 0;

        for (int[] section : coverage) {
            int j = 0;
            while ((j < section.length) && (elementsWritten < numElements)) {
                if (!inRun) {
                    boolean startRun = true;
                    runValue = section[j];
                    for (int k = 1; k < RLE_RUN_THRESHOLD; k++) {
                        if (j + k >= section.length || runValue != section[j + k]) {
                            startRun = false;
                            break;
                        }
                    }
                    if (startRun) {
                        runCount = RLE_RUN_THRESHOLD;
                        inRun = true;
                        j += runCount;
                    } else {
                        int value = section[j++];
                        out.writeInt(value == RLE_RUN_MARKER ? RLE_RUN_MARKER - 1 : value);
                        intsWritten++;
                    }
                } else {
                    if (runValue == section[j]) {
                        runCount++;
                        j++;
                    } else {
                        out.writeInt(RLE_RUN_MARKER);
                        out.writeInt(runCount);
                        out.writeInt(runValue == RLE_RUN_MARKER ? RLE_RUN_MARKER - 1 : runValue);
                        inRun = false;
                        intsWritten += 3;
                    }
                }
            }

            elementsWritten += j;
        }
        if (inRun) {
            out.writeInt(RLE_RUN_MARKER);
            out.writeInt(runCount);
            out.writeInt(runValue == RLE_RUN_MARKER ? RLE_RUN_MARKER - 1 : runValue);
            intsWritten += 3;
        }
        Logger.getInstance().debug("[wrote " + elementsWritten + " elements as " + intsWritten * 4 + " bytes (RLE)]");
    }

    public static void writeUncompressedCoverage(DataOutputStream out, int[][] coverage, int numElements) throws IOException {
        int written = 0;
        for (int[] section : coverage) {
            int j;
            for (j = 0; j < section.length && (written < numElements); j++) {
                out.writeInt(section[j]);
            }
            written += j;
        }
        Logger.getInstance().debug("[wrote " + written + " elements as " + coverage.length * 4 + " bytes (uncompressed)]");
    }

    public static int[] readCoverageAndSumCoverage(DataInputStream in, AtomicLong sum) throws IOException {
        // read dimensions of coverage data
        final int elementCount = in.readInt();
        final int[] elements = new int[elementCount];
        final byte[] data = new byte[elementCount * 4];

        long localSum = 0;
        int offset = 0;
        int read = 0;
        while (read != -1 && offset != data.length) {
            read = in.read(data, offset, data.length - offset);
            offset += read;
        }

        try {
            int j = 0;
            int i = 0;

            while (i < elementCount) {
                // reading ints manually this way instead of in.readInt() gets us a 40% speed up.
                final int m = (((data[j++] & 0xff) << 24) | ((data[j++] & 0xff) << 16) | ((data[j++] & 0xff) << 8) | (data[j++] & 0xff));
                // this code will read RLE compressed data or uncompressed data, since RUN_MARKER will only occur in RLE data
                if (m == RLE_RUN_MARKER) {
                    final int c = (((data[j++] & 0xff) << 24) | ((data[j++] & 0xff) << 16) | ((data[j++] & 0xff) << 8) | (data[j++] & 0xff));
                    final int v = (((data[j++] & 0xff) << 24) | ((data[j++] & 0xff) << 16) | ((data[j++] & 0xff) << 8) | (data[j++] & 0xff));
                    if (v != 0) {
                        Arrays.fill(elements, i, i + c, v);
                        localSum += c * v;
                    }
                    i += c;
                } else {
                    elements[i++] = m;
                    localSum += m;
                }
            }
            Logger.getInstance().debug("[read " + elementCount + " elements as " + (offset + 1) + " bytes with sum " + localSum + "]");
            sum.set(localSum);
            return elements;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Recording corrupt");
        }
    }
}
