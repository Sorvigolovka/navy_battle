package battleship;

import java.awt.Point;

class GameController {
    private final Board playerBoard;
    private final Board aiBoard;
    private final ComputerAI computerAI;
    private final StatisticsManager statisticsManager;

    private boolean playerTurn = true;
    private boolean playerOneTurn = true;
    private boolean gameOver = false;
    private boolean pendingLocalSwitch = false;
    private GameMode mode = GameMode.VS_AI;

    GameController(Board playerBoard, Board aiBoard) {
        this(playerBoard, aiBoard, GameMode.VS_AI, true, true, null, null);
    }

    GameController(Board playerBoard, Board aiBoard, GameMode mode) {
        this(playerBoard, aiBoard, mode, true, true, null, null);
    }

    GameController(Board playerBoard, Board aiBoard, GameMode mode, StatisticsManager statisticsManager) {
        this(playerBoard, aiBoard, mode, true, true, null, statisticsManager);
    }

    GameController(Board playerBoard, Board aiBoard, GameMode mode, boolean playerTurn, boolean playerOneTurn,
            ComputerAI existingAi, StatisticsManager statisticsManager) {
        this.playerBoard = playerBoard;
        this.aiBoard = aiBoard;
        this.mode = mode;
        this.playerTurn = playerTurn;
        this.playerOneTurn = playerOneTurn;
        this.statisticsManager = statisticsManager;
        this.computerAI = existingAi != null ? existingAi : new ComputerAI();
    }

    void resetGame() {
        playerBoard.reset();
        aiBoard.reset();
        playerTurn = true;
        playerOneTurn = true;
        gameOver = false;
        pendingLocalSwitch = false;
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
        handleShotResult(aiBoard, row, col, result);
        return result;
    }

    ShotResult aiFire() {
        if (mode != GameMode.VS_AI || playerTurn || gameOver) {
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
        if (gameOver || pendingLocalSwitch) {
            return ShotResult.already(row, col);
        }
        Board target = playerOneTurn ? aiBoard : playerBoard;
        ShotResult result = target.fireAt(row, col);
        handleShotResult(target, row, col, result);
        return result;
    }

    private void handleShotResult(Board target, int row, int col, ShotResult result) {
        if (result == null || result.getOutcome() == ShotOutcome.ALREADY || gameOver) {
            return;
        }
        if (result.getOutcome() == ShotOutcome.SUNK) {
            Ship sunkShip = result.getShip();
            if (sunkShip != null && target.containsShip(sunkShip)) {
                target.markSurroundingCellsAsMiss(sunkShip);
            }
        }
        if (target.allShipsSunk()) {
            concludeGame(target);
            return;
        }
        if (result.getOutcome() == ShotOutcome.MISS) {
            if (mode == GameMode.LOCAL_PVP) {
                pendingLocalSwitch = true;
            } else {
                switchCurrentPlayer();
            }
        }
    }

    private void concludeGame(Board defeatedBoard) {
        gameOver = true;
        pendingLocalSwitch = false;
        if (statisticsManager == null) {
            return;
        }
        if (mode == GameMode.VS_AI) {
            if (defeatedBoard == aiBoard) {
                statisticsManager.recordWin(GameMode.VS_AI);
            } else {
                statisticsManager.recordLoss(GameMode.VS_AI);
            }
        } else if (mode == GameMode.LOCAL_PVP) {
            if (defeatedBoard == aiBoard) {
                statisticsManager.recordWin(GameMode.LOCAL_PVP);
            } else {
                statisticsManager.recordLoss(GameMode.LOCAL_PVP);
            }
        }
    }

    private void switchCurrentPlayer() {
        if (mode == GameMode.LOCAL_PVP) {
            playerOneTurn = !playerOneTurn;
        } else {
            playerTurn = !playerTurn;
        }
    }

    void completeLocalSwitch() {
        if (mode != GameMode.LOCAL_PVP || !pendingLocalSwitch) {
            return;
        }
        pendingLocalSwitch = false;
        playerOneTurn = !playerOneTurn;
    }

    boolean isLocalSwitchPending() {
        return pendingLocalSwitch;
    }

    boolean isPlayerTurn() {
        return mode == GameMode.LOCAL_PVP ? playerOneTurn : playerTurn;
    }

    boolean isPlayerTurnFlag() {
        return playerTurn;
    }

    boolean isPlayerOneTurnFlag() {
        return playerOneTurn;
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

    ComputerAI getComputerAI() {
        return computerAI;
    }

    GameState createState(Language language) {
        return new GameState(playerBoard, aiBoard, playerTurn, playerOneTurn, mode, language, computerAI);
    }
}
