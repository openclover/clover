package com.atlassian.clover.reporters;

import java.io.File;

/**
 * Interface used by reporters to determine whether a file is to be included
 * in a report
 */
public interface ReportFilter {
    boolean isFileIncluded(File sourceFile);
}
