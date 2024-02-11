package com.atlassian.clover.registry;

import org.openclover.runtime.api.registry.CloverRegistryException;

import java.io.File;

public class NoSuchRegistryException extends CloverRegistryException {
    /**
     * Constructor with a standard error message.
     * @param filePath path to clover database
     */
    public NoSuchRegistryException(String filePath) {
        super(
            "Clover registry file \"" + filePath + "\" does not exist, cannot be read or is a directory. " +
            "\nPlease ensure Clover has instrumented your source files. " +
            "\nYou may need to remove existing .class files for this to occur.");
    }

    /**
     * Constructor with a custom message provided
     * @param messageFormat use "${file}" string for a file name
     * @param absFile path to clover database
     */
    public NoSuchRegistryException(String messageFormat, File absFile) {
        super(messageFormat.replace("${file}", absFile.getAbsolutePath()));
    }
}
