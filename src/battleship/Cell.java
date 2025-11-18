package battleship;

class Cell {
    private final int row;
    private final int col;
    private Ship ship;
    private boolean shot;

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
}
