package battleship;

enum ShotOutcome {
    MISS,
    HIT,
    SUNK,
    ALREADY
}

class ShotResult {
    private final int row;
    private final int col;
    private final ShotOutcome outcome;
    private final Ship ship;

    private ShotResult(int row, int col, ShotOutcome outcome, Ship ship) {
        this.row = row;
        this.col = col;
        this.outcome = outcome;
        this.ship = ship;
    }

    static ShotResult miss(int row, int col) {
        return new ShotResult(row, col, ShotOutcome.MISS, null);
    }

    static ShotResult hit(int row, int col, Ship ship, boolean sunk) {
        return new ShotResult(row, col, sunk ? ShotOutcome.SUNK : ShotOutcome.HIT, ship);
    }

    static ShotResult already(int row, int col) {
        return new ShotResult(row, col, ShotOutcome.ALREADY, null);
    }

    int getRow() {
        return row;
    }

    int getCol() {
        return col;
    }

    ShotOutcome getOutcome() {
        return outcome;
    }

    Ship getShip() {
        return ship;
    }
}
