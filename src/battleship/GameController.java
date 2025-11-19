package battleship;

import java.awt.Point;

class GameController {
    private final Board playerBoard;
    private final Board aiBoard;
    private final ComputerAI computerAI = new ComputerAI();
    private boolean playerTurn = true;
    private boolean gameOver = false;
    private GameMode mode = GameMode.VS_AI;
    private boolean playerOneTurn = true;

    GameController(Board playerBoard, Board aiBoard) {
        this.playerBoard = playerBoard;
        this.aiBoard = aiBoard;
    }

    GameController(Board playerBoard, Board aiBoard, GameMode mode) {
        this(playerBoard, aiBoard);
        this.mode = mode;
    }

    void resetGame() {
        playerBoard.reset();
        aiBoard.reset();
        playerTurn = true;
        gameOver = false;
        computerAI.reset();
        playerOneTurn = true;
    }

    ShotResult playerFire(int row, int col) {
        if (mode == GameMode.LOCAL_PVP) {
            return localPlayerFire(row, col);
        }
        if (!playerTurn || gameOver) {
            return ShotResult.already(row, col);
        }
        ShotResult result = aiBoard.fireAt(row, col);
        handleShotResult(aiBoard, row, col, result);
        return result;
    }

    ShotResult aiFire() {
        if (mode != GameMode.VS_AI) {
            return null;
        }
        if (playerTurn || gameOver) {
            return null;
        }
        ShotResult result = null;
        while (!playerTurn && !gameOver) {
            Point target = computerAI.chooseTarget(playerBoard);
            result = playerBoard.fireAt(target.x, target.y);
            computerAI.handleShotResult(target, result);
            handleShotResult(playerBoard, target.x, target.y, result);
        }
        return result;
    }

    private ShotResult localPlayerFire(int row, int col) {
        Board target = playerOneTurn ? aiBoard : playerBoard;
        if (gameOver) {
            return ShotResult.already(row, col);
        }
        ShotResult result = target.fireAt(row, col);
        handleShotResult(target, row, col, result);
        return result;
    }

    private void handleShotResult(Board target, int row, int col, ShotResult result) {
        if (result.getOutcome() == ShotOutcome.ALREADY) {
            return;
        }
        if (result.getOutcome() == ShotOutcome.SUNK) {
            Ship sunkShip = result.getShip();
            if (sunkShip != null) {
                target.markSurroundingCellsAsMiss(sunkShip);
            }
        }
        if (target.allShipsSunk()) {
            gameOver = true;
            return;
        }
        if (result.getOutcome() == ShotOutcome.MISS) {
            switchCurrentPlayer();
        }
    }

    private void switchCurrentPlayer() {
        if (mode == GameMode.LOCAL_PVP) {
            playerOneTurn = !playerOneTurn;
        } else {
            playerTurn = !playerTurn;
        }
    }

    boolean isPlayerTurn() {
        return mode == GameMode.LOCAL_PVP ? playerOneTurn : playerTurn;
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

    GameMode getMode() {
        return mode;
    }
}
