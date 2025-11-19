package battleship;

import java.io.Serializable;

class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Board playerOneBoard;
    private final Board playerTwoBoard;
    private final boolean playerTurn;
    private final boolean playerOneTurn;
    private final GameMode mode;
    private final Language language;
    private final ComputerAI computerAI;

    GameState(Board p1, Board p2, boolean playerTurn, boolean playerOneTurn, GameMode mode, Language language,
            ComputerAI computerAI) {
        this.playerOneBoard = p1;
        this.playerTwoBoard = p2;
        this.playerTurn = playerTurn;
        this.playerOneTurn = playerOneTurn;
        this.mode = mode;
        this.language = language;
        this.computerAI = computerAI;
    }

    Board getPlayerOneBoard() {
        return playerOneBoard;
    }

    Board getPlayerTwoBoard() {
        return playerTwoBoard;
    }

    boolean isPlayerOneTurn() {
        return playerOneTurn;
    }

    boolean isPlayerTurn() {
        return playerTurn;
    }

    GameMode getMode() {
        return mode;
    }

    Language getLanguage() {
        return language;
    }

    ComputerAI getComputerAi() {
        return computerAI;
    }
}
