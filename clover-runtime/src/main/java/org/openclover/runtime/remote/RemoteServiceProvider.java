package org.openclover.runtime.remote;

/**
 */
public interface RemoteServiceProvider {

    Config createConfig(String serverLocation);

    RecorderService createService(Config config);

    RecorderListener createListener(Config config);
    
}
