package com.atlassian.clover.remote;

/**
 * A Registry Service provides a mechanism for exposing a Clover Recorder to remote JVMs.
 * One or more RecorderListeners may register with a RecoriderService to receive flush events. 
 *
 */
public interface RecorderService {


    void init(Config config);
    void start();
    void stop();
    Object sendMessage(RpcMessage message);
    
}
