package com.atlassian.clover.instr.java;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Data source which reads from CharSequence
 */
public class CharSequenceInstrumentationSource implements InstrumentationSource {

    private final File sourceFile;
    private final CharSequence charSequence;

    /**
     *
     * @param sourceFile location of the source file
     * @param charSequence content of the source file
     */
    public CharSequenceInstrumentationSource(final File sourceFile, final CharSequence charSequence) {
        this.sourceFile = sourceFile;
        this.charSequence = charSequence;
    }

    @Override
    public File getSourceFileLocation() {
        return sourceFile;
    }

    @Override
    public Reader createReader() {
        return new StringReader(charSequence.toString());
    }
}
