package org.openclover.idea.junit;

import org.openclover.runtime.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketUtils {
    public static void close(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Logger.getInstance().verbose("Failed to close socket: " + socket, e);
            }
        }
    }

    public static void close(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Logger.getInstance().verbose("Failed to close socket: " + socket, e);
            }
        }
    }
}
