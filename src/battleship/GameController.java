package battleship;

import java.awt.Point;

class GameController {
    private final Board playerBoard;
    private final Board aiBoard;
    private final ComputerAI computerAI = new ComputerAI();
    private boolean playerTurn = true;
    private boolean gameOver = false;

    GameController(Board playerBoard, Board aiBoard) {
        this.playerBoard = playerBoard;
        this.aiBoard = aiBoard;
    }

    void resetGame() {
        playerBoard.reset();
        aiBoard.reset();
        playerTurn = true;
        gameOver = false;
        computerAI.reset();
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
        Point target = computerAI.chooseTarget(playerBoard);
        ShotResult result = playerBoard.fireAt(target.x, target.y);
        computerAI.handleShotResult(target, result, playerBoard);
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
}
