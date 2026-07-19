package org.openclover.runtime.remote;

/**
 * A Registry Service provides a mechanism for exposing a Clover Recorder to remote JVMs.
 * One or more RecorderListeners may register with a RecoriderService to receive flush events. 
 *
 */
public interface RecorderService {


    void init(Config config);
    void start();
    void stop();

    /**
     * Broadcasts a coverage event to all connected clients.
     *
     * @return the number of clients that successfully applied the event
     */
    int sendMessage(RpcMessage message);
    
}
