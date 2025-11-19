package battleship;

import java.io.Serializable;

class Cell implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int row;
    private final int col;
    private Ship ship;
    private boolean shot;
    private boolean miss;

    Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    int getRow() {
        return row;
    }

    int getCol() {
        return col;
    }

    Ship getShip() {
        return ship;
    }

    void setShip(Ship ship) {
        this.ship = ship;
    }

    boolean hasShip() {
        return ship != null;
    }

    boolean isShot() {
        return shot;
    }

    void markShot() {
        this.shot = true;
    }

    void setShot(boolean shot) {
        this.shot = shot;
    }

    boolean isMiss() {
        return miss;
    }

    void setMiss(boolean miss) {
        this.miss = miss;
    }
}
