package com.atlassian.clover.remote;

/**
 */
public interface RemoteServiceProvider {

    Config createConfig(String serverLocation);

    RecorderService createService(Config config);

    RecorderListener createListener(Config config);
    
}
