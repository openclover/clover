package org.openclover.runtime.remote;

import gnu.cajo.invoke.Remote;
import gnu.cajo.invoke.RemoteInvoke;
import gnu.cajo.utils.extra.ItemProxy;
import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.Logger;
import org_openclover_runtime.Clover;

import java.rmi.ConnectException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A client to a remote registry.
 * <p/>
 * This class is used for connecting to, maintaining the connection to, and sending messages to a remote Clover registry.
 *
 * @see CajoTcpRecorderService
 */
public class CajoTcpRecorderListener implements RecorderListener {

    private DistributedConfig config;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final Timer reconnectionTimer = new Timer(true);
    
    @Override
    public void init(Config tcpConfig) {
        this.config = (DistributedConfig) tcpConfig;
    }

    /**
     * A wrapper method for the same method on the Clover class to add extra logging.
     * Remebmer to update {@link RpcMessage#METHODS} if you change method's signature.
     * @see Clover#allRecordersSliceStart(String, int, long)
     */
    public void allRecordersSliceStart(String type, Integer slice, Long startTime) {
        Clover.allRecordersSliceStart(type, slice, startTime);
    }

    /**
     * A wrapper method for the same method on the Clover class to allow for extra logging.
     * Remebmer to update {@link RpcMessage#METHODS} if you change method signature.
     *
     * @see Clover#allRecordersSliceEnd(String, String, String, int, int, ErrorInfo)
     */
    public void allRecordersSliceEnd(String type, String method, String runtimeTestName,
                                     Integer slice, Integer i, ErrorInfo ei) {
        Clover.allRecordersSliceEnd(type, method, runtimeTestName, slice, i, ei);
    }

    @Override
    public Object handleMessage(RpcMessage message) {
        // nothing to be done here since the allRecorders* methods are invoked via reflection already via Cajo.
        return "SUCCESS";
    }

    /**
     * This method is called by Cajo when the connection to the server is lost.
     *
     * @param x the exception that caused the connection to be lost. if null, then the server was shutdown.
     */
    public void cutOff(Exception x) {
        Logger.getInstance().debug("cutOff(" + x + "); from: " + config.getServerLocation() + ". Attempting to reconnect.");
        reconnect();
    }


    @Override
    public void connect() {
        reconnect();
    }

    /**
     * Stop attempting to reconnect by cancelling the reconnection timer.
     * This call will block until all previously queued tasks have finished.
     */
    @Override
    public void disconnect() {
        reconnectionTimer.cancel();
    }

    private boolean connectToServer() {
        final String url = getConnectionUrl();
        try {
            Logger.getInstance().debug("Attempting connection to: " + url);
            final Object server = Remote.getItem(url);


            Logger.getInstance().debug("Received remote item: " + server + " from: " + url);


            Logger.getInstance().debug("Invoking remote method: " + CajoTcpRecorderService.REGISTER_CALLBACK);
            final RemoteInvoke result = (RemoteInvoke) Remote.invoke(server, CajoTcpRecorderService.REGISTER_CALLBACK, null);

            Logger.getInstance().debug("Received result: " + result);

            final ItemProxy proxy = new ItemProxy(result, this);
            Logger.getInstance().debug("Started proxy: " + proxy.getName());
            // the result need not be kept as a reference once the ItemProxy has been created.
            return true;
        } catch (ConnectException e) {
            Logger.getInstance().debug("Could not connect to server at " + url + ". " + e.getMessage(), e);
        } catch (Exception e) {
            Logger.getInstance().error("Error while connecting to: " + url + " : " + e, e);
        }
        return false;
    }

    public String getConnectionUrl() {
        return "//" + config.getServerLocation() + "/" + config.getName();
    }

    /**
     * Attempts to connect to the server JVM.
     * <p/>
     * As soon as a connection is made, the thread exits. This is used to allow Clover to connect to the server while
     * also letting the application continue loading.
     */
    private void reconnect() {
        if (reconnecting.getAndSet(true)) {
            return;
        }

        reconnectionTimer.schedule(new ReconnectTimerTask(), 0, config.getRetryPeriod());
        Logger.getInstance().debug("Started timer to attempt reconnect every: " + config.getRetryPeriod() + " ms.");
    }

    private class ReconnectTimerTask extends TimerTask {
        @Override
        public void run() {
            if (connectToServer()) {
                reconnecting.set(false);
                reconnectionTimer.cancel();
                this.cancel(); // ensure this is run no more
            }
        }
    }
}
