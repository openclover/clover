package com.atlassian.clover.instr.java;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;

/**
 * Data source reading from a File.
 */
public class FileInstrumentationSource implements InstrumentationSource {

    private final File sourceFile;
    private final String encoding;

    /**
     *
     * @param sourceFile location of the source file
     * @param encoding  charset encoding or <code>null</code>
     */
    public FileInstrumentationSource(final File sourceFile, final String encoding) {
        this.sourceFile = sourceFile;
        this.encoding = encoding;
    }

    @Override
    public File getSourceFileLocation() {
        return sourceFile;
    }

    @Override
    public Reader createReader() throws IOException {
        if (encoding != null) {
            return new InputStreamReader(Files.newInputStream(sourceFile.toPath()), encoding);
        } else {
            return new FileReader(sourceFile);
        }
    }
}
