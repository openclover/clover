package com.atlassian.clover.instr.java;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Data source which reads from a String
 */
public class StringInstrumentationSource implements InstrumentationSource {

    private final File sourceFile;
    private final String content;

    /**
     *
     * @param sourceFile location of the source file
     * @param content content of the source file
     */
    public StringInstrumentationSource(final File sourceFile, final String content) {
        this.sourceFile = sourceFile;
        this.content = content;
    }

    @Override
    public File getSourceFileLocation() {
        return sourceFile;
    }

    @Override
    public Reader createReader() {
        return new StringReader(content);
    }
}
