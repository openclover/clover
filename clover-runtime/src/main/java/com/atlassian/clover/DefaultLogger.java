package com.atlassian.clover;

import java.io.PrintStream;

public class DefaultLogger extends Logger {
    private static String[] LOG_LEVEL_STR = {
        "ERROR",
        "WARN",
        "INFO",
        "VERBOSE",
        "DEBUG"
    };

    @Override
    public void log(int level, String msg, Throwable t) {
        if (canIgnore(level)) {
            return;
        }

        final PrintStream stream;
        final String decoratedMsg;
        if (level >= LOG_INFO ) {
            // for INFO or below level msgs, print the msg only, nothing else
            decoratedMsg = msg;
            stream = System.out;
        } else {
            // since we are obfuscating, mName shouldn't be printed.
            decoratedMsg = LOG_LEVEL_STR[level] + ": " + msg;
            stream = System.err;            
        }
        stream.print(decoratedMsg);

        if (t != null && (isVerbose() || isDebug())) {
            System.err.println(msg + " : " + t.getMessage());
            t.printStackTrace();
        } else {
            stream.println();
        }
    }
}
