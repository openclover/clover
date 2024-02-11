package org.openclover.runtime.recorder;

import java.io.IOException;

/**
 * Coverage recording from the currently running application
 */
public interface LiveGlobalCoverageRecording extends GlobalCoverageRecording {
    /**
     * Write the coverage in some way to somewhere, returning a memento of the operation (to log)
     */
    String write() throws IOException;
}
