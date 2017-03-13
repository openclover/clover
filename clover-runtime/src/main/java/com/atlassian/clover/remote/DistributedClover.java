package com.atlassian.clover.remote;

import com.atlassian.clover.CloverProperties;
import com.atlassian.clover.Logger;
import com.atlassian.clover.CloverNames;
import com_atlassian_clover.CloverProfile;

import java.util.Arrays;

public class DistributedClover {
    private final RecorderService service;
    private final RecorderListener client;

    private final boolean serverMode;

    public DistributedClover(CloverProperties cloverProperties, CloverProfile profile) {
        serverMode = cloverProperties.getBooleanProperty(CloverNames.PROP_SERVER, false);
        RecorderService service = null;
        RecorderListener client = null;
        try {
            // distributed coverage from profile (clover.profile + source instrumentation) has priority over
            //  - setting from system property (clover.distributed.coverage) and
            //  - old-style source instrumentation (nvpProperties)
            final String distributedConfigString;
            if (profile != null && profile.getDistributedCoverage() != null) {
                // read config string from a current profile
                distributedConfigString =  profile.getDistributedCoverage().getConfigString();
            } else {
                // the distributed config, as set via a system property. if no property set, use the the instr. config.
                distributedConfigString = cloverProperties.getProperty(CloverNames.PROP_DISTRIBUTED_CONFIG);
            }

            if (distributedConfigString == null) {
                Logger.getInstance().verbose("Distributed coverage is disabled.'");
                return;
            }

            final Config config = RemoteFactory.getInstance().createConfig(distributedConfigString);

            if (!config.isEnabled()) {
                Logger.getInstance().verbose("Distributed coverage is disabled via config: " + distributedConfigString);
                return;
            }

            Logger.getInstance().info("Distributed coverage is enabled with: " + config);

            if (serverMode) {
                client = null;
                service = createServer(config);
            } else {
                service = null;
                client = createClient(config);
            }
        } catch (Exception e) {
            Logger.getInstance().error("Could not initialise Distributed Coverage collection in Clover: " + e.getMessage(), e);
            client = null;
            service = null;
        } finally {
            this.client = client;
            this.service = service;
        }
    }

    public void remoteFlush(RpcMessage message) {
        if (serverMode && service != null) {
            if (Logger.isDebug()) {
                Logger.getInstance().debug(message.getName() + "( " + Arrays.toString(message.getMethodArgs()) + ")");
            }

            final long remstart = System.currentTimeMillis();
            service.sendMessage(message);

            if (Logger.isDebug()) {
                Logger.getInstance().debug(message.getName() + " remote flush took: " + (System.currentTimeMillis() - remstart) + " ms");
            }
        }
    }

    private RecorderService createServer(Config config) {
        Logger.getInstance().info("Starting distributed coverage service.");
        RecorderService service = RemoteFactory.getInstance().createService(config);
        service.start();
        return service;
    }


    private RecorderListener createClient(Config config) {
        // begin trying to connect to the SERVICE. Don't block however, to let the application continue loading.
        RecorderListener client = RemoteFactory.getInstance().createListener(config);
        Logger.getInstance().info("Starting distributed coverage client: " + config);
        client.connect();
        return client;
    }

    public void stop() {
        synchronized (this) {
            if (client != null) {
                client.disconnect();
            }

            if (service != null) {
                service.stop();
            }
        }
    }

    boolean isServiceMode() {
        return service != null;
    }
}
