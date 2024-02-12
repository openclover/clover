package org.openclover.idea.junit;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.junit.config.OptimizedConfigurationSettings;
import org.openclover.runtime.Logger;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class SocketBasedJUnitClassListProcessor extends JUnitClassListProcessor implements Closeable {

    /* How long we may wait for socket creation. Timeout in ms. */
    private static final int SOCKET_CONNECT_DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(1);

    /* How long we may wait for a list of test classes. Timeout in ms. */
    private static final int JUNIT_CLASS_LIST_DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    private final int junitClassListTimeout;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    public SocketBasedJUnitClassListProcessor(SavingsReporter savingsReporter, File ideaGeneratedFile,
                                              Project currentProject, OptimizedConfigurationSettings optimizationSettings,
                                              int junitClassListTimeout) {
        super(savingsReporter, ideaGeneratedFile, currentProject, optimizationSettings);
        this.junitClassListTimeout = junitClassListTimeout;
    }

    public SocketBasedJUnitClassListProcessor(SavingsReporter savingsReporter, File ideaGeneratedFile,
                                              Project currentProject, OptimizedConfigurationSettings optimizationSettings) {
        this(savingsReporter, ideaGeneratedFile, currentProject, optimizationSettings, JUNIT_CLASS_LIST_DEFAULT_TIMEOUT);
    }

    public int processWhenSocketReady(final int socket) {
        clientSocket = new Socket();
        try {
            clientSocket.connect(new InetSocketAddress(InetAddress.getByName(null), socket), SOCKET_CONNECT_DEFAULT_TIMEOUT);
        } catch (IOException e) {
            Logger.getInstance().warn("Cannot connect to JUnit runner synchronization socket", e);
            return -1;
        }
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            Logger.getInstance().warn("Cannot create proxy JUnit runner synchronization socket", e);
            close();
            return -1;
        }


        final Task.Backgroundable task = new Task.Backgroundable(currentProject, "Clover Test Optimization Task", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Waiting for JUnit class list");
                try {
                    final DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    clientSocket.setSoTimeout(junitClassListTimeout);
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
        return serverSocket.getLocalPort();
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
