package org.openclover.core;

import java.util.EventListener;

/**
 *
 */
public interface ProgressListener extends EventListener {

    void handleProgress(String desc, float pc);

    ProgressListener NOOP_LISTENER = (desc, pc) -> {
        // no op
    };
}
