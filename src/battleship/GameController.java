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
    }

    ShotResult playerFire(int row, int col) {
        if (mode == GameMode.LOCAL_PVP) {
            return localPlayerFire(row, col);
        }
        if (!playerTurn || gameOver) {
            return ShotResult.already(row, col);
        }
        ShotResult result = aiBoard.fireAt(row, col);
        if (result.getOutcome() != ShotOutcome.ALREADY) {
            if (result.getOutcome() == ShotOutcome.SUNK) {
                aiBoard.markSurroundingCellsAsMiss(result.getShip());
            }
            if (aiBoard.allShipsSunk()) {
                gameOver = true;
            } else if (result.getOutcome() == ShotOutcome.MISS) {
                playerTurn = false;
            }
        }
        return result;
    }

    ShotResult aiFire() {
        if (mode != GameMode.VS_AI) {
            return null;
        }
        if (gameOver) {
            return null;
        }
        Point target = computerAI.chooseTarget(playerBoard);
        ShotResult result = playerBoard.fireAt(target.x, target.y);
        computerAI.handleShotResult(target, result, playerBoard);
        if (result.getOutcome() == ShotOutcome.SUNK) {
            playerBoard.markSurroundingCellsAsMiss(result.getShip());
        }
        if (playerBoard.allShipsSunk()) {
            gameOver = true;
        } else if (result.getOutcome() == ShotOutcome.MISS) {
            playerTurn = true;
        }
        return result;
    }

    private ShotResult localPlayerFire(int row, int col) {
        Board target = playerOneTurn ? aiBoard : playerBoard;
        if (gameOver) {
            return ShotResult.already(row, col);
        }
        ShotResult result = target.fireAt(row, col);
        if (result.getOutcome() != ShotOutcome.ALREADY) {
            if (result.getOutcome() == ShotOutcome.SUNK) {
                target.markSurroundingCellsAsMiss(result.getShip());
            }
            if (target.allShipsSunk()) {
                gameOver = true;
            } else if (result.getOutcome() == ShotOutcome.MISS) {
                playerOneTurn = !playerOneTurn;
            }
        }
        return result;
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
