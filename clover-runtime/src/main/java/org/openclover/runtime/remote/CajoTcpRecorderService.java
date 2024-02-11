package org.openclover.runtime.remote;


import clover.gnu.cajo.invoke.Remote;
import clover.gnu.cajo.utils.ItemServer;
import clover.gnu.cajo.utils.extra.ClientProxy;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.Formatting;

import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Instances of this class are used to start a service which exposes specific methods to clients running in separate JVMs.
 * Each instance of this class on the same machine will need to have a unique port number.
 */
public class CajoTcpRecorderService implements RecorderService {

    private final List<ClientProxy> clientProxies = new CopyOnWriteArrayList<>();
    private DistributedConfig config;

    private static final int INIT_SLEEP_MILLIS = 500;
    private Remote server;

    @Override
    public void init(Config config) {
        this.config = (DistributedConfig) config;
    }

    @Override
    public void start() {
        try {
            Logger.getInstance().debug("About to start service with config: " + config);
            Remote.config(config.getHost(), config.getPort(), null, 0);

            server = ItemServer.bind(this, config.getName());
            Logger.getInstance().debug("Started coverage service: " + config.getName());
            // wait until all the clients connect
            if (config.getNumClients() > 0) {
                Logger.getInstance().info("Clover waiting for " + config.getNumClients() +
                        " remote clients to attach to this remote testing session. ");
            }

            // this allows a server to be cleanly started, and then the tests run
            while (clientProxies.size() < config.getNumClients()) {
                try {
                    Thread.sleep(INIT_SLEEP_MILLIS);
                    Logger.getInstance().debug("Waiting for " + config.getNumClients() + " remote VMs " + clientProxies.size());
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

            Logger.getInstance().debug("Recording proceeding now that " + Formatting.pluralizedVal(clientProxies.size(), "client") + " are connected.");

        } catch (RemoteException | UnknownHostException e) {
            Logger.getInstance().error("Error starting recorder service: " + config, e);
        }
    }

    /**
     * Stop the service and notify any remote clients
     */
    @Override
    public void stop() {
        if (server != null) {
            try {
                server.unexport(true);
            } catch (NoSuchObjectException e) {
                Logger.getInstance().info("Error stopping service: " + e.getMessage());
            }
        }
    }

    static final String REGISTER_CALLBACK = "registerListener";

    /**
     * This method is invoked when a remote client wishes to register itself with this service.
     *
     * @return a reference to be used by the client to create an ItemProxy
     */
    public Remote registerListener() {

        Logger.getInstance().verbose("registerListener(). proxies: " + clientProxies);
        try {
            ClientProxy proxy = new ClientProxy();

            clientProxies.add(proxy);

            Logger.getInstance().debug("Accepting connection from client: " + UnicastRemoteObject.getClientHost());
            return new Remote(proxy);
        } catch (RemoteException | ServerNotActiveException e) {
            Logger.getInstance().error("Error registering listener.", e);
        }
        return null;
    }

    @Override
    public Object sendMessage(RpcMessage message) {
        final int numClients = invokeAllClients(message.getName(), message.getMethodArgs());
        Logger.getInstance().debug("Invoked method " + message.getName() + " on " + numClients + " remote clients.");
        return numClients;
    }

    private int invokeAllClients(String methodName, Object parameters) {
        int numSuccess = 0;

        for (final ClientProxy rec : clientProxies) {
            rec.timeout = config.getTimeout(); // the only means to set the timeout.
            try {
                rec.remoteThis.invoke(methodName, parameters);
                numSuccess++;
            } catch (Exception e) {
                Logger.getInstance().warn(" Error occured during a remote flush to: " +
                        methodName + " on " + rec.remoteThis +
                        ". message: " + e.getMessage() + " - " + e, e);
                clientProxies.remove(rec);
            }
        }
        return numSuccess;
    }

    public int getNumRegisteredListeners() {
        return clientProxies.size();
    }
}
