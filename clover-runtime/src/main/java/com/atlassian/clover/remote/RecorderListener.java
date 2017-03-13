package com.atlassian.clover.remote;

/**
 * A RecorderClient is used to accept flush messages from a remote jvm and process them. 
 *
 */
public interface RecorderListener {

    void init(Config config);
    Object handleMessage(RpcMessage message);
    void connect();
    void disconnect();
}
