package battleship;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class GameController {
    private final Board playerBoard;
    private final Board aiBoard;
    private final Random random = new Random();
    private final List<Point> aiTargets = new ArrayList<>();
    private boolean playerTurn = true;
    private boolean gameOver = false;

    GameController(Board playerBoard, Board aiBoard) {
        this.playerBoard = playerBoard;
        this.aiBoard = aiBoard;
        refillAiTargets();
    }

    void resetGame() {
        playerBoard.reset();
        aiBoard.reset();
        playerTurn = true;
        gameOver = false;
        refillAiTargets();
    }

    ShotResult playerFire(int row, int col) {
        if (!playerTurn || gameOver) {
            return ShotResult.already(row, col);
        }
        ShotResult result = aiBoard.fireAt(row, col);
        if (result.getOutcome() != ShotOutcome.ALREADY) {
            if (aiBoard.allShipsSunk()) {
                gameOver = true;
            } else {
                playerTurn = false;
            }
        }
        return result;
    }

    ShotResult aiFire() {
        if (gameOver) {
            return null;
        }
        if (aiTargets.isEmpty()) {
            refillAiTargets();
        }
        Point target = aiTargets.remove(random.nextInt(aiTargets.size()));
        ShotResult result = playerBoard.fireAt(target.x, target.y);
        if (playerBoard.allShipsSunk()) {
            gameOver = true;
        } else {
            playerTurn = true;
        }
        return result;
    }

    boolean isPlayerTurn() {
        return playerTurn;
    }

    boolean isGameOver() {
        return gameOver;
    }

    Board getPlayerBoard() {
        return playerBoard;
    }

    Board getAiBoard() {
        return aiBoard;
    }

    private void refillAiTargets() {
        aiTargets.clear();
        aiTargets.addAll(playerBoard.availableTargets());
    }
}
