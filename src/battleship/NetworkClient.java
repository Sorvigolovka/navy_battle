package battleship;

import java.io.IOException;
import java.net.Socket;

class NetworkClient {
    private final String host;
    private final int port;

    NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    Socket connect() throws IOException {
        return new Socket(host, port);
    }
}
