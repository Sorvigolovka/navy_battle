package battleship;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

class OnlineMatch {
    interface Listener {
        void onOpponentReady();
        void onLocalShotResult(ShotResult result);
        void onIncomingShot(ShotResult result);
        void onTurnChanged(boolean yourTurn);
        void onGameOver(boolean localWon);
        void onNetworkError(String message);
        void requestBoardRefresh();
    }

    private final GameController controller;
    private final Listener listener;
    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final Thread readerThread;
    private volatile boolean running = true;
    private volatile boolean finished = false;
    private volatile boolean localReady;
    private volatile boolean remoteReady;
    private PendingShot pendingShot;

    OnlineMatch(GameController controller, Listener listener, Socket socket) throws IOException {
        this.controller = controller;
        this.listener = listener;
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        readerThread = new Thread(this::listen, "online-listener");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    void markLocalReady() {
        if (localReady) {
            return;
        }
        localReady = true;
        send("READY");
        checkStart();
    }

    boolean isReadyToPlay() {
        return localReady && remoteReady;
    }

    void fireShot(int row, int col) {
        if (!isReadyToPlay() || pendingShot != null || controller.isGameOver()) {
            return;
        }
        pendingShot = new PendingShot(row, col);
        send("SHOT " + row + " " + col);
    }

    void disconnect() {
        send("DISCONNECT");
        shutdown();
    }

    void shutdown() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void listen() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                final String message = line.trim();
                if (message.isEmpty()) {
                    continue;
                }
                SwingUtilities.invokeLater(() -> processMessage(message));
            }
        } catch (IOException ex) {
            if (running) {
                SwingUtilities.invokeLater(() -> listener.onNetworkError(ex.getMessage()));
            }
        } finally {
            running = false;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void processMessage(String message) {
        if (message.startsWith("READY")) {
            remoteReady = true;
            listener.onOpponentReady();
            checkStart();
            return;
        }
        if (message.startsWith("SHOT")) {
            handleIncomingShot(message);
            return;
        }
        if (message.startsWith("RESULT")) {
            handleResult(message);
            return;
        }
        if (message.startsWith("GAME_OVER")) {
            handleGameOver(message);
            return;
        }
        if (message.startsWith("DISCONNECT")) {
            controller.concludeOnlineGame(true);
            listener.onGameOver(true);
            shutdown();
        }
    }

    private void handleIncomingShot(String message) {
        String[] parts = message.split(" ");
        if (parts.length < 3) {
            return;
        }
        try {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            ShotResult result = controller.applyIncomingOnlineShot(row, col);
            send(buildResultMessage(result));
            listener.onIncomingShot(result);
            listener.requestBoardRefresh();
            if (controller.isGameOver()) {
                send("GAME_OVER WIN");
                listener.onGameOver(false);
                shutdown();
            } else {
                listener.onTurnChanged(controller.isPlayerTurn());
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleResult(String message) {
        if (pendingShot == null) {
            return;
        }
        String[] parts = message.split(" ", 3);
        if (parts.length < 2) {
            return;
        }
        ShotOutcome outcome;
        try {
            outcome = ShotOutcome.valueOf(parts[1]);
        } catch (IllegalArgumentException ex) {
            return;
        }
        List<Point> sunk = parts.length == 3 ? parsePoints(parts[2]) : new ArrayList<>();
        ShotResult result = controller.applyRemoteShotResult(pendingShot.row, pendingShot.col, outcome, sunk);
        pendingShot = null;
        listener.onLocalShotResult(result);
        listener.requestBoardRefresh();
        if (controller.isGameOver() && !finished) {
            finished = true;
            listener.onGameOver(true);
            shutdown();
        } else {
            listener.onTurnChanged(controller.isPlayerTurn());
        }
    }

    private void handleGameOver(String message) {
        String[] parts = message.split(" ");
        if (finished) {
            return;
        }
        boolean localWin = parts.length > 1 && "WIN".equalsIgnoreCase(parts[1]);
        controller.concludeOnlineGame(localWin);
        finished = true;
        listener.onGameOver(localWin);
        shutdown();
    }

    private void send(String text) {
        synchronized (writer) {
            writer.println(text);
            writer.flush();
        }
    }

    private void checkStart() {
        if (isReadyToPlay()) {
            listener.onTurnChanged(controller.isPlayerTurn());
            listener.requestBoardRefresh();
        }
    }

    private String buildResultMessage(ShotResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("RESULT ").append(result.getOutcome().name());
        if (result.getOutcome() == ShotOutcome.SUNK && result.getShip() != null) {
            builder.append(' ').append(formatPoints(result.getShip()));
        }
        return builder.toString();
    }

    private String formatPoints(Ship ship) {
        StringBuilder builder = new StringBuilder();
        for (Cell cell : ship.getCells()) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(cell.getRow()).append(',').append(cell.getCol());
        }
        return builder.toString();
    }

    private List<Point> parsePoints(String payload) {
        List<Point> points = new ArrayList<>();
        if (payload == null || payload.isEmpty()) {
            return points;
        }
        String[] entries = payload.split(";");
        for (String entry : entries) {
            String[] rc = entry.split(",");
            if (rc.length != 2) {
                continue;
            }
            try {
                points.add(new Point(Integer.parseInt(rc[0]), Integer.parseInt(rc[1])));
            } catch (NumberFormatException ignored) {
            }
        }
        return points;
    }

    private static class PendingShot {
        final int row;
        final int col;

        PendingShot(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
}
