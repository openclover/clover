package com.atlassian.clover.idea;

import com.atlassian.clover.Logger;

public class IdeaLogger extends Logger {

    private com.intellij.openapi.diagnostic.Logger ideaLog;

    public IdeaLogger(com.intellij.openapi.diagnostic.Logger ideaLog) {
        super();
        this.ideaLog = ideaLog;
    }


    @Override
    public void log(int level, String aMsg, Throwable t) {

        switch (level) {
            case Logger.LOG_VERBOSE:
            case Logger.LOG_DEBUG:
                ideaLog.debug(aMsg);
                if (t != null) {
                    ideaLog.debug(t);
                }
                break;

            case Logger.LOG_INFO:
                ideaLog.info(aMsg);
                if (t != null) {
                    ideaLog.info(t);
                }
                break;

            case Logger.LOG_ERR:
                if (t != null) {
                    ideaLog.info(aMsg, t);
                } else {
                    ideaLog.info(aMsg);
                }
                break;

            default:
                ideaLog.debug("<unknown log level> " + aMsg);
                break;
        }
    }
}

