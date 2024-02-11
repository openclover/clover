package org.openclover.core.instr.java;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * A data source from which we can get a file and its content to be instrumented.
 */
public interface InstrumentationSource {
    /**
     * Returns a location of the source file. This is just a description and there's no guarantee that the file actually
     * exists, therefore a consumer of the IntrumentationSource shall not try to read this file directly. Use createReader
     * instead.
     *
     * @return File - location of the source file
     */
    File getSourceFileLocation();

    /**
     * Creates an instance of a Reader which allows to get the content of the file.
     *
     * @return Reader - to get the content
     */
    Reader createReader() throws IOException;

}
