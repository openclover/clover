package com.atlassian.clover.ant.tasks;

import org.apache.tools.ant.Task;

public class CloverWarningTask extends Task {

    static {
        System.err.println("WARNING: the 'clovertasks' and 'clovertypes' resources " +
                "are no longer used and have been replaced by 'cloverlib.xml'. \n" +
                "To load Clover, you must now use: \n" +
                "<taskdef resource=\"cloverlib.xml\" classpath=\"/path/to/clover.jar\"/>");
    }

}