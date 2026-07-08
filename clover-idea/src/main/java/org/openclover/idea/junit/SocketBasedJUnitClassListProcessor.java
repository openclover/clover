package org.openclover.idea.junit;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.junit.config.OptimizedConfigurationSettings;
import org.openclover.runtime.Logger;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;

public class SocketBasedJUnitClassListProcessor extends JUnitClassListProcessor implements Closeable {

    /* How long we may wait for IDEA to bind its synchronization socket before giving up. */
    private static final Duration SOCKET_CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /* How often we retry connecting while IDEA has not bound its synchronization socket yet. */
    private static final Duration SOCKET_CONNECT_POLL_INTERVAL = Duration.ofMillis(10);

    /* How long we may wait for a list of test classes. */
    private static final Duration JUNIT_CLASS_LIST_DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final Duration junitClassListTimeout;
    private Duration socketConnectTimeout = SOCKET_CONNECT_TIMEOUT;
    private Duration socketConnectPollInterval = SOCKET_CONNECT_POLL_INTERVAL;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    public SocketBasedJUnitClassListProcessor(SavingsReporter savingsReporter, File ideaGeneratedFile,
                                              Project currentProject, OptimizedConfigurationSettings optimizationSettings,
                                              Duration junitClassListTimeout) {
        super(savingsReporter, ideaGeneratedFile, currentProject, optimizationSettings);
        this.junitClassListTimeout = junitClassListTimeout;
    }

    public SocketBasedJUnitClassListProcessor(SavingsReporter savingsReporter, File ideaGeneratedFile,
                                              Project currentProject, OptimizedConfigurationSettings optimizationSettings) {
        this(savingsReporter, ideaGeneratedFile, currentProject, optimizationSettings, JUNIT_CLASS_LIST_DEFAULT_TIMEOUT);
    }

    /**
     * Test seam: shortens the connect timeout and poll interval so the "IDEA never binds its socket" give-up path
     * can be exercised quickly. Production code relies on the {@link #SOCKET_CONNECT_TIMEOUT} /
     * {@link #SOCKET_CONNECT_POLL_INTERVAL} defaults.
     */
    void setSocketConnectTimeouts(Duration timeout, Duration pollInterval) {
        this.socketConnectTimeout = timeout;
        this.socketConnectPollInterval = pollInterval;
    }

    public int processWhenSocketReady(final int socket) {
        // Create our proxy socket now and rewrite the run parameter to point at it, but defer connecting to IDEA's
        // own synchronization socket. Since IDEA 2024+ that socket is bound lazily - in
        // JavaTestFrameworkRunnableState.resolveServerSocketPort(), inside createHandler() - which runs *after* the
        // program runner has patched the run parameters.
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            Logger.getInstance().warn("Cannot create proxy JUnit runner synchronization socket", e);
            close();
            return -1;
        }

        // Read the port up front: the background task may run and close the socket
        final int proxyPort = serverSocket.getLocalPort();

        final Task.Backgroundable task = new Task.Backgroundable(currentProject, "OpenClover Test Optimization Task", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Connecting to JUnit runner");
                if (!connectToJUnitSyncSocket(socket)) {
                    onCancel();
                    return;
                }

                indicator.setText("Waiting for JUnit class list");
                try {
                    final DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    clientSocket.setSoTimeout((int) junitClassListTimeout.toMillis());
                    final boolean wireValue = dataInputStream.readBoolean();
                    dataInputStream.close();

                    processFile();

                    notifyJUnitListener(wireValue);
                } catch (IOException e) {
                    throw new ProcessCanceledException(e);
                }

                indicator.setText("Optimizing tests");
                if (!processFile()) {
                    onCancel();
                }
            }

            @SuppressWarnings({"ResultOfMethodCallIgnored"})
            @Override
            public void onCancel() {
                close();
            }

        };
        task.queue();
        return proxyPort;
    }

    /**
     * Connects to IDEA's JUnit runner synchronization socket, retrying while the port is not bound yet.
     * IDEA binds this socket lazily (only when it builds the process handler), which happens after our program runner
     * has patched the run parameters, so an immediate connection fails with 'Connection refused'.
     *
     * @param port IDEA's synchronization socket port
     * @return true if connected, false if the socket could not be reached within the timeout
     */
    boolean connectToJUnitSyncSocket(final int port) {
        final InetSocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(InetAddress.getByName(null), port);
        } catch (UnknownHostException e) {
            Logger.getInstance().warn("Cannot resolve JUnit runner synchronization socket host", e);
            return false;
        }

        final long deadline = System.currentTimeMillis() + socketConnectTimeout.toMillis();
        while (true) {
            final int remaining = (int) (deadline - System.currentTimeMillis());
            if (remaining <= 0) {
                Logger.getInstance().warn("Timed out after " + socketConnectTimeout.toMillis()
                        + " ms waiting for JUnit runner synchronization socket at " + socketAddress);
                return false;
            }
            clientSocket = new Socket();
            try {
                // Bound each attempt by the time left so the total never exceeds socketConnectTimeout.
                clientSocket.connect(socketAddress, remaining);
                return true;
            } catch (ConnectException e) {
                // IDEA has not bound its synchronization socket yet - wait socketConnectPollInterval and retry.
                SocketUtils.close(clientSocket);
                clientSocket = null;
                ThreadUtils.sleepQuietly(socketConnectPollInterval);
            } catch (IOException e) {
                Logger.getInstance().warn("Cannot connect to JUnit runner synchronization socket: " + socketAddress, e);
                SocketUtils.close(clientSocket);
                clientSocket = null;
                return false;
            }
        }
    }

    void notifyJUnitListener(boolean wireValue) throws IOException {
        final Socket socket = serverSocket.accept();
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeBoolean(wireValue);
        dataOutputStream.close();
    }

    @Override
    public void close() {
        if (serverSocket != null) {
            SocketUtils.close(serverSocket);
            serverSocket = null;
        }
        // we can rely on JUnit runner closing the server side of this connection so we don't care that much.
        if (clientSocket != null) {
            SocketUtils.close(clientSocket);
            clientSocket = null;
        }
    }
}
