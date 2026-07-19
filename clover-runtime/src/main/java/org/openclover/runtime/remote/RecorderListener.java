package org.openclover.runtime.remote;

/**
 * A RecorderClient is used to accept flush messages from a remote jvm and process them.
 */
public interface RecorderListener {
    void init(Config config);

    void connect();

    void disconnect();
}
