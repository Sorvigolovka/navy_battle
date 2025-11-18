package battleship;

import java.io.Serializable;

class GameState implements Serializable {
    private final Board playerOneBoard;
    private final Board playerTwoBoard;
    private final boolean playerOneTurn;
    private final GameMode mode;
    private final Language language;

    GameState(Board p1, Board p2, boolean playerOneTurn, GameMode mode, Language language) {
        this.playerOneBoard = p1;
        this.playerTwoBoard = p2;
        this.playerOneTurn = playerOneTurn;
        this.mode = mode;
        this.language = language;
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

    GameMode getMode() {
        return mode;
    }

    Language getLanguage() {
        return language;
    }
}
