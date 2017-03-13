package com.atlassian.clover;

import java.util.EventListener;

/**
 *
 */
public interface ProgressListener extends EventListener {

    void handleProgress(String desc, float pc);

    public final static ProgressListener NOOP_LISTENER = new ProgressListener() {
        @Override
        public void handleProgress(String desc, float pc) {
            // no op
        }
    };
}
