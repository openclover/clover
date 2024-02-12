package org.openclover.runtime.recorder;

import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.CoverageUtils;
import org.openclover.runtime.util.FOSFactory;
import org.openclover.runtime.util.IOStreamUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class FileBasedGlobalCoverageRecording extends BaseCoverageRecording implements LiveGlobalCoverageRecording {
    /**
     * force pre-emptive loading of classes referenced in this class,
     * so they aren't lazily loaded in the shutdown hook, which can cause
     * problems (namely for Tomcat).
     */
    static final Class<?>[] REQUIRED_CLASSES = {
        IOException.class, DataOutputStream.class, OutputStream.class,
        FileNotFoundException.class, BufferedOutputStream.class,
        FileOutputStream.class, Deflater.class, DeflaterOutputStream.class, FOSFactory.class,
        FOSFactory.REQUIRED_CLASSES.getClass(), Header.class, File.class, IOStreamUtils.class,
        CoverageUtils.class, ArrayIndexOutOfBoundsException.class
    };

    private static final boolean USE_RLE_COMPRESSION =
            Boolean.parseBoolean(System.getProperty(CloverNames.PROP_RLE_COVERAGE, Boolean.TRUE.toString()));

    private final int[][] elements;
    private final int numElements;

    public FileBasedGlobalCoverageRecording(String path, long dbVersion, long timeStamp, int[][] elements, int numElements) {
        super(new Header(dbVersion, timeStamp, GlobalCoverageRecording.FORMAT), new File(path));
        this.elements = elements;
        this.numElements = numElements;
    }

    @Override
    public String write() throws IOException {
        Logger.getInstance().verbose("Writing global coverage file " + fileOnDisk.getAbsolutePath());
        File file = createCoverageFolderFor(fileOnDisk);

        DataOutputStream out = new DataOutputStream(IOStreamUtils.createDeflateOutputStream(file));
        try {
            header.write(out);
            out.writeInt(numElements);
            if (USE_RLE_COMPRESSION) {
                CoverageUtils.rleCompressAndWriteCoverage(out, elements, numElements);
            } else {
                CoverageUtils.writeUncompressedCoverage(out, elements, numElements);
            }
            out.flush();
        } finally {
            IOStreamUtils.close(out);
        }
        return file.getAbsolutePath();
    }

    static void flushToDisk(String dbname, long dbversion, long writeTS, int[] elements) throws IOException {
        new FileBasedGlobalCoverageRecording(dbname, dbversion, writeTS, new int[][] {elements}, elements.length).write();
    }
}
