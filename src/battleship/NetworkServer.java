package battleship;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class NetworkServer {
    private final int port;
    private ServerSocket serverSocket;

    NetworkServer(int port) {
        this.port = port;
    }

    Socket waitForClient() throws IOException {
        serverSocket = new ServerSocket(port);
        return serverSocket.accept();
    }

    void close() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
